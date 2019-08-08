package poc

import akka.Done
import akka.actor.ActorRef
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.Source
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

  override def sleepAfterProduce: FiniteDuration = 10.seconds

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
      importe= 1,
      tipoMovimiento= 'C',
      fechaUltMod= DateTime.now
    )
    def updateObligacion(
                          evoId: String,
                          objetoId: String,
                          sujetoId: String,
                          aggregateRoot: String,
                          deliveryId: Long,
                        ) =   UpdateMovimiento(
      aggregateRoot: String,
      deliveryId:    Long,
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

    awaitProduce(produce(appConfig.SOURCE_TOPIC, requests))



    val flow = txFlow.controlGraph(obligacionRef, objetoRef, sujetoRef)
    flow
      .run()
      .streamCompletion
      .foreach {
      a =>

        for {
          oblid <- 1 to 3
          objid <- 1 to 2
          sid <- 1 to 2
        } yield for {
          objectState <- (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
          sujetoState <- (sujetoRef ? AggregateObjeto.GetState(sid.toString)).mapTo[AggregateObjeto.StateObjeto]

        } yield ()

        for {
          objectState <- (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
          sujetoState <- (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
          objectState <- (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
        } yield ()
        val state = (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
        println(s"Expected saldo is: ${expectedSaldo * N * 2}, and saldo is ${a}")
    }

    val state = (objetoRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
    state.foreach { a =>
      println(s"Expected saldo is: ${expectedSaldo * N * 2}, and saldo is ${a.saldo}")
      assert(a.saldo == expectedSaldo * N * 2)
      assert(
        a.obligaciones.collect {
          case (_, o@Obligacion(_, "1", saldo, _)) if saldo == expectedSaldo * N => o
        }.size == 2
      )
    }
  }



}
