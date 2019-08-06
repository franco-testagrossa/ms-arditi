package defaults.kafka

import akka.actor
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.ExecutionContextExecutor

object KafkaConsumerDefaults {
  import defaults.akkaDefaults._

  val system: actor.ActorSystem = actorSystem("KafkaConsumer")

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val config = ConfigFactory.load()
  private val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

}
