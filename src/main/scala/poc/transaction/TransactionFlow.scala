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
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.model.TX

object TransactionFlow {
  // Control Flow Settings
  private def consumerSettings[A](system: ActorSystem): ConsumerSettings[String, A] =
    ConsumerSettings(system, new StringDeserializer, new KafkaDeserializer[A])
      .withBootstrapServers("config.KAFKA_BROKER") // TODO
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId("DATA_GROUP") // TODO

  private def producerSettings[A](system: ActorSystem): ProducerSettings[String, A] =
    ProducerSettings(system, new StringSerializer, new KafkaSerializer[A])
      .withBootstrapServers("bootstrapServers") // TODO

  private val sourceTopic = Set("sourceTopic") // TODO
  private val sinkTopic = "sinkTopic" // TODO

  private def transactionalId: String = java.util.UUID.randomUUID().toString
  def controlGraph[A,B,C,D](
                    objeto: ActorRefFlowStage[TX[A], TX[B]],
                    sujeto: ActorRefFlowStage[TX[C], TX[D]]
                  )(mapper: TX[B] => TX[C])
                   (implicit system: ActorSystem): RunnableGraph[DrainingControl[Done]] = {
    val consumer = consumerSettings[A](system)
    val producer = producerSettings[C](system)
    Transactional
      .source(consumer, Subscriptions.topics(sourceTopic))
      .via(objeto)
      .map(mapper)
      .via(sujeto)
      .map { msg =>
        ProducerMessage.single(
          new ProducerRecord(sinkTopic, msg.record.key, msg.record.value), msg.partitionOffset
        )
      }
      .toMat(Transactional.sink(producer, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
  }

}
