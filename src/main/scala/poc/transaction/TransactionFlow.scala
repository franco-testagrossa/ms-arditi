package poc.transaction

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.{ConsumerMessage, ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Consumer, Transactional}
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Keep, RunnableGraph, Source}
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.AppConfig
import poc.kafka.{KafkaDeserializer, KafkaSerializer}

import scala.concurrent.{Await, ExecutionContext, Future}
import akka.pattern.ask
import org.joda.time.DateTime
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.{Obligacion, UpdateObligacion}
import poc.model.{objeto, obligacion, sujeto}
import poc.model.obligacion.AggregateObligacion
import poc.model.obligacion.AggregateObligacion.UpdateMovimiento
import poc.model.sujeto.AggregateSujeto
import poc.model.sujeto.AggregateSujeto.Objeto

import scala.collection.immutable


class TransactionFlow(config: AppConfig)(implicit system: ActorSystem) {
  import config._

  private def consumerSettings[A]: ConsumerSettings[String, A] =
    ConsumerSettings(system, new StringDeserializer, new KafkaDeserializer[A])
      .withBootstrapServers(KAFKA_BROKER)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId(CONSUMER_GROUP)

  def producerSettings[A]: ProducerSettings[String, A] =
    ProducerSettings(system, new StringSerializer, new KafkaSerializer[A])
      .withBootstrapServers(KAFKA_BROKER)

  private def transactionalId: String = java.util.UUID.randomUUID().toString

  val toUpdateObligacion = { o: AggregateObligacion.UpdateSuccess =>
    AggregateObjeto.UpdateObligacion(
      aggregateRoot = o.movimiento.objetoId,
      deliveryId =    o.deliveryId,
      obligacion =    Obligacion(
        obligacionId =     o.aggregateRoot,
        sujetoId =         o.movimiento.sujetoId,
        saldoObligacion =  o.movimiento.importe,
        fechaUltMod =      o.movimiento.fechaUltMod
      )
    )
  }
  def toUpdateObjeto(sujetoId: String, saldoObjeto: Double) = { o: AggregateObjeto.UpdateSuccess =>
    AggregateSujeto.UpdateObjeto(
      aggregateRoot = sujetoId,
      deliveryId = o.deliveryId,
      objeto = Objeto(
        objetoId = o.aggregateRoot,
        sujetoId = sujetoId,
        saldoObjeto = saldoObjeto,
        fechaUltMod = DateTime.now()
      ))
  }
  def controlGraph(
                    obligacion: ActorRef,
                    objeto: ActorRef,
                    sujeto: ActorRef
                  )
                  (implicit t: Timeout, ec: ExecutionContext): RunnableGraph[DrainingControl[Done]] = {

    val consumer = consumerSettings[UpdateObligacion]
    val producer = producerSettings[Seq[AggregateSujeto.UpdateSuccess]]

    val transaction = Transactional
      .source(consumer, Subscriptions.topics(SOURCE_TOPIC))

    val consumerObligacion = consumerSettings[UpdateMovimiento]
    val movimientoTransaction = Transactional
      .source(consumerObligacion, Subscriptions.topics(SOURCE_TOPIC))

    val obligacionStage = movimientoTransaction.watch(obligacion)
      .mapAsync(1) {
        case txa: ConsumerMessage.TransactionalMessage[String, UpdateMovimiento] =>


          (obligacion ? txa.record.value())
            .mapTo[AggregateObligacion.UpdateSuccess]
            .map(x => (txa, x))
      }
      .mapError {
        // the purpose of this recovery is to change the name of the stage in that exception
        // we do so in order to help users find which stage caused the failure -- "the ask stage"
        case ex: WatchedActorTerminatedException =>
          throw new WatchedActorTerminatedException("ask()", ex.ref)
      }
      .named("ask_obligacion")

    val objetoStage:
      Source[(ConsumerMessage.TransactionalMessage[String, UpdateMovimiento],
        AggregateObjeto.UpdateSuccess), Consumer.Control]
    = obligacionStage.watch(objeto)
      .mapAsync(1) {
        case (
          txa: ConsumerMessage.TransactionalMessage[String, UpdateMovimiento],
          success: AggregateObligacion.UpdateSuccess
          ) =>

          (objeto ? toUpdateObligacion(success))
            .mapTo[AggregateObjeto.UpdateSuccess]
            .map(x => (txa, x))
      }
      .mapError {
        // the purpose of this recovery is to change the name of the stage in that exception
        // we do so in order to help users find which stage caused the failure -- "the ask stage"
        case ex: WatchedActorTerminatedException =>
          throw new WatchedActorTerminatedException("ask()", ex.ref)
      }
      .named("ask_sujeto")


    val sujetoStage = objetoStage.watch(sujeto)
      .mapAsync(1) {
        case (
          txa: ConsumerMessage.TransactionalMessage[String, UpdateMovimiento],
          success: AggregateObjeto.UpdateSuccess
          ) =>

          var a =
            success
              .sujetos
              .map { case (sid, saldo) => toUpdateObjeto(sid, saldo)(success) }
              .toSeq
              .map { request =>
                (sujeto ? request)
                  .mapTo[AggregateSujeto.UpdateSuccess]
              }

          Future.sequence(a).map(x => txa -> x)
      }
      .mapError {
        // the purpose of this recovery is to change the name of the stage in that exception
        // we do so in order to help users find which stage caused the failure -- "the ask stage"
        case ex: WatchedActorTerminatedException =>
          throw new WatchedActorTerminatedException("ask()", ex.ref)
      }
      .named("ask_sujeto")

    val commitedStage = sujetoStage.map {
        case (
          txa: ConsumerMessage.TransactionalMessage[String, UpdateMovimiento],
          success: Seq[AggregateSujeto.UpdateSuccess]
          ) =>

          ProducerMessage.single(
            new ProducerRecord(SINK_TOPIC, txa.record.key, success), txa.partitionOffset
          )
      }
      .toMat(Transactional.sink(producer, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)

    commitedStage
  }
}
