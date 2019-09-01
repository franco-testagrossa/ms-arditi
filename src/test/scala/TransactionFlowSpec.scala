
import akka.Done
import akka.actor.ActorRef
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.pattern.ask
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import com.typesafe.config.ConfigFactory
import config.AppConfig
import model.ddd.GetState
import model.payment.PaymentActor
import model.payment.PaymentActor.UpdateMovimiento
import model.person.PersonActor
import model.product.ProductActor
import org.apache.kafka.clients.producer.ProducerRecord
import transaction.TransactionFlow

import scala.collection.immutable
import scala.concurrent.Future

class TransactionFlowSpec extends DocsSpecBase {


  import scala.concurrent.ExecutionContext

  implicit val ecc = ExecutionContext.Implicits.global

  import utils.generators.Model._

  def transactionFlowTest(requests: immutable.Seq[UpdateMovimiento]) = {
    val payment: ActorRef = PaymentActor.start
    val product: ActorRef = ProductActor.start
    val person: ActorRef = PersonActor.start

    lazy val config = ConfigFactory.load()
    val appConfig = new AppConfig(config)
    val txFlow = new TransactionFlow(appConfig)


    val flow = txFlow.controlGraph(payment, product, person)

    def produce(topic: String, data: immutable.Seq[PaymentActor.UpdateMovimiento]): Future[Done] = {
      import io.circe.syntax._
      import model.payment.Serializer._
      val simulatedRealDataAsItWouldComeFromKafka = data.map {
        _.asJson.spaces2
      }

      Source(simulatedRealDataAsItWouldComeFromKafka)
        .map(n =>
          new ProducerRecord[String, String]("stage1", n))
        .runWith(Producer.plainSink(producer))
    }

    val control: Consumer.Control = flow._1
    val result: Future[Seq[Seq[String]]] = flow._2

    produce("stage1", requests).flatMap {
      case Done =>
        Thread.sleep(timeout.duration.toMillis)
        control.shutdown()
        for {
          a <- (person ? GetState("1")).mapTo[PersonActor.State]
          b <- (person ? GetState("2")).mapTo[PersonActor.State]
        } yield (a, b)
    }


  }


  "TransactionFlow" should "person- product- payment" in assertAllStagesStopped {
    val requests: immutable.Seq[UpdateMovimiento] = (for {
      person <- 1 to 4
      product <- 1 to 2
      payment <- 1 to 2
    } yield updatePayment(
      paymentId = "1",
      productId = product.toString,
      personId = person.toString,
      aggregateRoot = payment.toString,
      deliveryId = 1L
    ))
      .zipWithIndex
      .map { case (a: UpdateMovimiento, i: Int) => a.copy(deliveryId = i + 1000) }

    val results: Future[(PersonActor.State, PersonActor.State)] = transactionFlowTest(requests)
    results map {
      case (a, b) =>
        println(s"Result: a is: $a")
        println(s"Result: b is: $b")
        assert(a.balance == b.balance)
    }
  }

  "TransactionFlow" should "product- person -payment" in assertAllStagesStopped {
    val requests: immutable.Seq[UpdateMovimiento] = for {
      product <- 1 to 2
      person <- 1 to 2
      payment <- 1 to 3
    } yield updatePayment(
      paymentId = "1",
      productId = product.toString,
      personId = person.toString,
      aggregateRoot = payment.toString,
      deliveryId = 1L
    )

    val results = transactionFlowTest(requests)
    results map {
      case (a, b) =>
        println(s"Result: a is: $a")
        println(s"Result: b is: $b")
        assert(a.balance == b.balance)
    }
  }

  "TransactionFlow" should "product- payment - person" in {
    val requests: immutable.Seq[UpdateMovimiento] = for {
      product <- 1 to 5
      payment <- 1 to 5
      person <- 1 to 5
    } yield updatePayment(
      paymentId = "1",
      productId = product.toString,
      personId = person.toString,
      aggregateRoot = payment.toString,
      deliveryId = 1L
    )
    println(requests)

    val results = transactionFlowTest(requests)
    results map {
      case (a, b) =>
        println(s"Result: a is: $a")
        println(s"Result: b is: $b")
        assert(a.balance === b.balance)
    }
  }

}
