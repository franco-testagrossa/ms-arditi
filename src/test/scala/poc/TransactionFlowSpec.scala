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

  override def sleepAfterProduce: FiniteDuration = 20.seconds

  "TransactionFlow" should "work" in assertAllStagesStopped {
    import scala.collection.JavaConverters._
    val config: Config = ConfigFactory.parseMap(Map("kafka.brokers" -> bootstrapServers).asJava)
    val appConfig = new AppConfig(config)
    val obligacionRef: ActorRef = AggregateObligacion.start
    val objetoRef: ActorRef = AggregateObjeto.start
    val sujetoRef: ActorRef = AggregateSujeto.start


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
      aggregateRoot: String,
      deliveryId: Long,
      movimiento = exampleMovimiento(evoId, objetoId, sujetoId)
    )

    val N = 10
    val expectedSaldo = 1

    val requests = for {
      did <- 1 to N //
      oblid <- 1 to 3
      objid <- 1 to 2
      sid <- 1 to 2
    } yield updateObligacion(
      evoId = did.toString,
      objetoId = objid.toString,
      sujetoId = sid.toString,
      aggregateRoot = oblid.toString,
      deliveryId = did.toLong
    )

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


    val state = (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
    state.foreach{ s =>
      println(s"Expected saldo is: ${s}")
    }
    val state2 = (objetoRef ? AggregateObjeto.GetState("2")).mapTo[AggregateObjeto.StateObjeto]
    state2.foreach{ s =>
      println(s"Expected saldo is: ${s}")
    }

    val state3 = (sujetoRef ? AggregateSujeto.GetState("1")).mapTo[AggregateSujeto.StateSujeto]
    state3.foreach{ s =>
      println(s"Expected saldo is: ${s}")
    }
    val state4 = (sujetoRef ? AggregateSujeto.GetState("2")).mapTo[AggregateSujeto.StateSujeto]
    state4.foreach{ s =>
      println(s"Expected saldo is: ${s}")
    }
  }



}
