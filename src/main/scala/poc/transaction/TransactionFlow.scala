package poc.transaction

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.Transactional
import akka.stream.scaladsl.{Keep, RunnableGraph}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.model.{ModelRequest, ModelResponse, TX}

class TransactionFlow(system: ActorSystem) {
  // Control Flow Settings
  private val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new KafkaDeserializer[ModelRequest])
      .withBootstrapServers("config.KAFKA_BROKER") // TODO
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId("DATA_GROUP") // TODO

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new KafkaSerializer[ModelResponse])
      .withBootstrapServers("bootstrapServers") // TODO

  private val sourceTopic = Set("sourceTopic") // TODO
  private val sinkTopic = "sinkTopic" // TODO
  private def transactionalId: String = java.util.UUID.randomUUID().toString

  def controlGraph(
                    aggregateObject: ActorRefFlowStage[TX[ModelRequest], TX[ModelResponse]]
                  ): RunnableGraph[DrainingControl[Done]] =
    Transactional
      .source(consumerSettings, Subscriptions.topics(sourceTopic))
      .map { msg: TX[ModelRequest] => msg }
      .via(aggregateObject)
      .map { msg: TX[ModelResponse] =>
        ProducerMessage.single(
          new ProducerRecord(sinkTopic, msg.record.key, msg.record.value), msg.partitionOffset
        )
      }
      .toMat(Transactional.sink(producerSettings, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
}
