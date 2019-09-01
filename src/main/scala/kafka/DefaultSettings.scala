package kafka

import akka.actor.ActorSystem
import akka.kafka.{ ConsumerSettings, ProducerSettings }
import com.typesafe.config.ConfigFactory
import config.AppConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }

object DefaultSettings {
  val config = ConfigFactory.load()
  val appConfig = new AppConfig(config)
  val bootstrapServers = appConfig.KAFKA_BROKER

  implicit def consumerSettings(implicit system: ActorSystem): ConsumerSettings[String, String] =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId(appConfig.CONSUMER_GROUP)
      .withBootstrapServers(bootstrapServers)

  implicit def producerSettings(implicit system: ActorSystem): ProducerSettings[String, String] =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
}

