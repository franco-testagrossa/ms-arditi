package common.kafka

import akka.actor
import akka.kafka.{ ConsumerSettings, ProducerSettings }
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import common.cqrs.utils.WriteOrientedActorSystem
import org.apache.kafka.common.serialization.{ StringDeserializer, StringSerializer }

import scala.concurrent.ExecutionContextExecutor

object KafKaConsumerDefaults extends WriteOrientedActorSystem {

  override val port = 2551
  override val lead = false
  override val index = 1

  val system: actor.ActorSystem = createActorSystem()

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val config = ConfigFactory.load()
  private val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

}
