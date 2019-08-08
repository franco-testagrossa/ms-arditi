package poc

import akka.Done
import akka.actor.ActorRef
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.DateTime
import poc.transaction.TransactionFlow
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.Obligacion
import poc.model.obligacion.AggregateObligacion
import poc.model.obligacion.AggregateObligacion.{Movimiento, UpdateMovimiento}
import poc.model.sujeto.AggregateSujeto
import poc.model.sujeto.AggregateSujeto.Objeto

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.ask
import poc.model.obligacion

class TransactionFlowSpec extends DocsSpecBase(KafkaPorts.ScalaTransactionsExamples) {

  def exampleMovimiento(
                         evoId: String,
                         objetoId: String,
                         sujetoId: String) = Movimiento(
    evoId,
    objetoId,
    sujetoId,
    importe = 1,
    tipoMovimiento = 'C',
    fechaUltMod = DateTime.now
  )

  def updateObligacion(
                        evoId: String,
                        objetoId: String,
                        sujetoId: String,
                        aggregateRoot: String,
                        deliveryId: Long,
                      ) = UpdateMovimiento(
    aggregateRoot,
    deliveryId,
    movimiento = exampleMovimiento(evoId, objetoId, sujetoId)
  )

  def transactionFlowTest(requests: immutable.Seq[UpdateMovimiento], N: Int):
  Future[(AggregateSujeto.StateSujeto, AggregateSujeto.StateSujeto)] = {
    import scala.collection.JavaConverters._
    val config: Config = ConfigFactory.parseMap(Map("kafka.brokers" -> bootstrapServers).asJava)
    val appConfig = new AppConfig(config)
    val obligacionRef: ActorRef = AggregateObligacion.start
    val objetoRef: ActorRef = AggregateObjeto.start
    val sujetoRef: ActorRef = AggregateSujeto.start







    val txFlow = new TransactionFlow(appConfig)

    def produce(topic: String, range: immutable.Seq[AggregateObligacion.UpdateMovimiento]): Future[Done] =
      Source(range)
        // NOTE: If no partition is specified but a key is present a partition will be chosen
        // using a hash of the key. If neither key nor partition is present a partition
        // will be assigned in a round-robin fashion.
        .map(n => new ProducerRecord(topic, partition0, DefaultKey, n))
        .runWith(Producer.plainSink(txFlow.producerSettings))

    val flow = txFlow.controlGraph(obligacionRef, objetoRef, sujetoRef)
    val control = flow.run()

    awaitProduce(produce(appConfig.SOURCE_TOPIC, requests))

    control.shutdown().futureValue should be(Done)
    control.drainAndShutdown()



    for {
      a <- (sujetoRef ? AggregateSujeto.GetState("1")).mapTo[AggregateSujeto.StateSujeto]
      b <- (sujetoRef ? AggregateSujeto.GetState("2")).mapTo[AggregateSujeto.StateSujeto]
    } yield (a, b)




  }

  override def sleepAfterProduce: FiniteDuration = 20.seconds






  "TransactionFlow" should "sujeto- objeto- obligacion" in assertAllStagesStopped {
    val requests: immutable.Seq[UpdateMovimiento] = for {
      sujeto <- 1 to 2
      objeto <- 1 to 2
      obligacion <- 1 to 3
    } yield updateObligacion(
      evoId = "1",
      objetoId = objeto.toString,
      sujetoId = sujeto.toString,
      aggregateRoot = obligacion.toString,
      deliveryId = 1L
    )

    val results = transactionFlowTest(requests, 1)
    results.foreach{
      case (a,b) =>
        println(s"Result: a is: ${a}")
        println(s"Result: b is: ${b}")
        assert(a.saldo == b.saldo)
    }
  }


  "TransactionFlow" should "objeto- sujeto -obligacion" in assertAllStagesStopped {
    val requests: immutable.Seq[UpdateMovimiento] = for {
      objeto <- 1 to 2
      sujeto <- 1 to 2
      obligacion <- 1 to 3
    } yield updateObligacion(
      evoId = "1",
      objetoId = objeto.toString,
      sujetoId = sujeto.toString,
      aggregateRoot = obligacion.toString,
      deliveryId = 1L
    )

    val results = transactionFlowTest(requests, 1)
    results.foreach{
      case (a,b) =>
        println(s"Result: a is: ${a}")
        println(s"Result: b is: ${b}")
        assert(a.saldo == b.saldo)
    }
  }


  "TransactionFlow" should "objeto- obligacion - sujeto" in assertAllStagesStopped {
    val requests: immutable.Seq[UpdateMovimiento] = for {
      objeto <- 1 to 5
      obligacion <- 1 to 5
      sujeto <- 1 to 5
    } yield updateObligacion(
      evoId = "1",
      objetoId = objeto.toString,
      sujetoId = sujeto.toString,
      aggregateRoot = obligacion.toString,
      deliveryId = 1L
    )
    println(requests)

    val results = transactionFlowTest(requests, 1)
    results.foreach{
      case (a,b) =>
        println(s"Result: a is: ${a}")
        println(s"Result: b is: ${b}")
        assert(a.saldo === b.saldo)
    }
  }

}
