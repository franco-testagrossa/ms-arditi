import akka.kafka.testkit.internal.TestFrameworkInterface
import akka.kafka.testkit.scaladsl.{EmbeddedKafkaLike, KafkaSpec}
import org.scalatest.{FlatSpecLike, Matchers, Suite}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

package object poc {
  /**
    * Ports to use for Kafka and Zookeeper throughout integration tests.
    * Zookeeper is Kafka port + 1 if nothing else specified.
    */
  object KafkaPorts {
    val RetentionPeriodSpec = 9012
    val TransactionsSpec = 9022
    val ReconnectSpec = 9032
    val ReconnectSpecProxy = 9034
    val MultiConsumerSpec = 9042
    val ScalaPartitionExamples = 9052
    val ScalaTransactionsExamples = 9062
    val ScalaAvroSerialization = 9072
    val AssignmentTest = 9082
    val SerializationTest = 9092
    val JavaTransactionsExamples = 9102
    val ProducerExamplesTest = 9112
    val KafkaConnectionCheckerTest = 9122
    val PartitionAssignmentHandlerSpec = 9132
  }

  abstract class DocsSpecBase(kafkaPort: Int)
    extends KafkaSpec(kafkaPort, kafkaPort + 1, sagas.utils.ClusterArditiSystem.system)
      with EmbeddedKafkaLike
      with FlatSpecLike
      with TestFrameworkInterface.Scalatest
      with Matchers
      with ScalaFutures
      with Eventually {

    this: Suite =>

    override implicit def patienceConfig: PatienceConfig =
      PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(150, Millis)))

    import akka.util.Timeout
    implicit val timeout: Timeout = 10 seconds

  }
}