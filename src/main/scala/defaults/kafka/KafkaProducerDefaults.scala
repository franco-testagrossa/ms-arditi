package defaults.kafka

import akka.actor
import akka.kafka.ProducerSettings
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.ExecutionContextExecutor

object KafkaProducerDefaults {
  import defaults.akkaDefaults._

  private val config = ConfigFactory.parseString(
    s"""
       |akka.remote {
       |    netty.tcp {
       |      ip = "127.0.0.2"
       |      port = 2550
       |    }
       |}
     """.stripMargin
  )
  val system: actor.ActorSystem = actorSystem("KafkaProducer", 2550, config)

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val producerConfig = ConfigFactory.load().getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

}
