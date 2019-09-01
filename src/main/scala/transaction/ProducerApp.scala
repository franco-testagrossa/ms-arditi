package transaction

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.{Done, actor}
import com.typesafe.config.ConfigFactory
import io.circe.syntax._
import model.payment.PaymentActor
import model.payment.PaymentActor.{Movimiento, UpdateMovimiento}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object ProducerApp extends App {

  val customConf =
    ConfigFactory.parseString(s"""
         akka {
           cluster {
             seed-nodes = ["akka://ClusterArditi@127.0.0.1:2551"]
           }
         }
        """)
  val config = customConf.withFallback(ConfigFactory.load()).resolve()

  implicit val system: actor.ActorSystem = ActorSystem("ClusterArditi", config)

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  import kafka.DefaultSettings._

  //val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)
  val requests: immutable.Seq[UpdateMovimiento] = for {
    product <- 1 to 20
    person <- 1 to 3
    payment <- 1 to 30
  } yield updatePayment(
    paymentId = "1",
    productId = product.toString,
    personId = person.toString,
    aggregateRoot = payment.toString,
    deliveryId = 1L
  )
  val produce: Future[Done] = produce("stage1", requests)

  def produce(topic: String, data: immutable.Seq[PaymentActor.UpdateMovimiento]): Future[Done] = {

    import model.payment.Serializer._
    val simulatedRealDataAsItWouldComeFromKafka = data.map { _.asJson.spaces2 }

    Source(simulatedRealDataAsItWouldComeFromKafka)
      // NOTE: If no partition is specified but a key is present a partition will be chosen
      // using a hash of the key. If neither key nor partition is present a partition
      // will be assigned in a round-robin fashion.
      .map(n =>
      new ProducerRecord[String, String](topic, n))
      .runWith(Producer.plainSink(producerSettings))
  }

  def updatePayment(
                        paymentId: String,
                        productId: String,
                        personId: String,
                        aggregateRoot: String,
                        deliveryId: BigInt,
                      ) = UpdateMovimiento(
    aggregateRoot,
    deliveryId,
    movimiento = exampleMovimiento(paymentId, productId, personId)
  )

  def exampleMovimiento(
                         paymentId: String,
                         productId: String,
                         personId: String) = Movimiento(
    paymentId,
    productId,
    personId,
    money = 1,
  )

  produce onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}