import java.util.concurrent.TimeUnit

import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.kafka.testkit.internal.TestFrameworkInterface
import akka.kafka.testkit.scaladsl.{EmbeddedKafkaLike, KafkaSpec}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{AsyncFlatSpecLike, Matchers}
import utils.generators.Infrastructure._

import scala.concurrent.duration._

abstract class DocsSpecBase
  extends KafkaSpec(
    _kafkaPort = randomAvailablePort,
    zooKeeperPort = randomAvailablePort,
    actorSystem = randomActorSystem(randomAvailablePort)
  )
    with EmbeddedKafkaLike
    with AsyncFlatSpecLike
    with TestFrameworkInterface.Scalatest
    with Matchers
    with ScalaFutures
    with Eventually {


  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(150, Millis)))

  import akka.util.Timeout

  implicit val timeout: Timeout = 15 seconds

  override def cleanUp(): Unit = {
    testProducer.close(60, TimeUnit.SECONDS)
    cleanUpAdminClient()
  }


  implicit val consumer: ConsumerSettings[String, String] = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  implicit val producer: ProducerSettings[String, String] = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(bootstrapServers)
}


