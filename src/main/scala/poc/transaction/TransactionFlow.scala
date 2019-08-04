package poc.transaction

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.{ConsumerMessage, ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.Transactional
import akka.stream.WatchedActorTerminatedException
import akka.stream.scaladsl.{Keep, RunnableGraph}
import akka.util.Timeout
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.AppConfig
import poc.kafka.{KafkaDeserializer, KafkaSerializer}

import scala.concurrent.ExecutionContext
import akka.pattern.ask
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.UpdateObligacion
import poc.model.sujeto.AggregateSujeto


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

  def controlGraph(
                    objeto: ActorRef,
                    sujeto: ActorRef
                  )(f: AggregateObjeto.UpdateSuccess => AggregateSujeto.UpdateObjeto)
                  (implicit t: Timeout, ec: ExecutionContext): RunnableGraph[DrainingControl[Done]] = {
    val consumer = consumerSettings[UpdateObligacion]
    val producer = producerSettings[AggregateSujeto.UpdateSuccess]
    Transactional
      .source(consumer, Subscriptions.topics(SOURCE_TOPIC))
      .watch(objeto)
      .mapAsync(1) {
        case txa: ConsumerMessage.TransactionalMessage[String, UpdateObligacion] =>

          (objeto ? txa.record.value())
            .mapTo[AggregateObjeto.UpdateSuccess]
            .map(x => (txa, x))
      }
      .mapError {
        // the purpose of this recovery is to change the name of the stage in that exception
        // we do so in order to help users find which stage caused the failure -- "the ask stage"
        case ex: WatchedActorTerminatedException =>
          throw new WatchedActorTerminatedException("ask()", ex.ref)
      }
      .named("ask_objeto")
      .watch(sujeto)
      .mapAsync(1) {
        case (txa: ConsumerMessage.TransactionalMessage[String, UpdateObligacion],
              success: AggregateObjeto.UpdateSuccess) =>

          (sujeto ? f(success))
            .mapTo[AggregateSujeto.UpdateSuccess]
            .map(x => (txa, x))
      }
      .mapError {
        // the purpose of this recovery is to change the name of the stage in that exception
        // we do so in order to help users find which stage caused the failure -- "the ask stage"
        case ex: WatchedActorTerminatedException =>
          throw new WatchedActorTerminatedException("ask()", ex.ref)
      }
      .named("ask_sujeto")
      .map {
        case (txa: ConsumerMessage.TransactionalMessage[String, UpdateObligacion],
              msg: AggregateSujeto.UpdateSuccess) =>

          ProducerMessage.single(
            new ProducerRecord(SINK_TOPIC, txa.record.key, msg), txa.partitionOffset
          )
      }
      .toMat(Transactional.sink(producer, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
  }
}
