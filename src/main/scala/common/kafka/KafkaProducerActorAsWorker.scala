package common.kafka

import akka.actor
import akka.kafka.ProducerSettings
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import common.cqrs.utils.WriteOrientedActorSystem
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.ExecutionContextExecutor

object KafkaProducerActorAsWorker extends WriteOrientedActorSystem {

  override val lead: Boolean = false

  val system: actor.ActorSystem = createActorSystem()

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val config = ConfigFactory.load()
  private val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

}
