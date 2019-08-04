package poc.transaction

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.Transactional
import akka.stream.scaladsl.{Keep, RunnableGraph}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.AppConfig
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.model.TX
import scalaz.Functor

class TransactionFlow(config: AppConfig)(implicit system: ActorSystem) {
  import config._

  private def consumerSettings[A]: ConsumerSettings[String, A] =
    ConsumerSettings(system, new StringDeserializer, new KafkaDeserializer[A])
      .withBootstrapServers(KAFKA_BROKER)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId(CONSUMER_GROUP)

  private def producerSettings[A]: ProducerSettings[String, A] =
    ProducerSettings(system, new StringSerializer, new KafkaSerializer[A])
      .withBootstrapServers(KAFKA_BROKER)

  private def transactionalId: String = java.util.UUID.randomUUID().toString

  import poc.ddd._
  def controlGraph[A <: Command ,B <: Response, C <: Command , D <: Response](
                    objeto: ActorRefFlowStage[A, B],
                    sujeto: ActorRefFlowStage[C, D]
                  )(mapper: B => C)(implicit functor: Functor[TX]): RunnableGraph[DrainingControl[Done]] = {
    val consumer = consumerSettings[A]
    val producer = producerSettings[D]
    Transactional
      .source(consumer, Subscriptions.topics(SOURCE_TOPIC))
         .via(objeto)
         .map(fa => functor.map(fa)(mapper))
         .via(sujeto)
      .map { msg =>
        ProducerMessage.single(
          new ProducerRecord(SINK_TOPIC, msg.record.key, msg.record.value), msg.partitionOffset
        )
      }
      .toMat(Transactional.sink(producer, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
  }
}
