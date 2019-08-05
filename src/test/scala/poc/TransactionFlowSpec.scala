package poc

import akka.Done
import akka.actor.ActorRef
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.DateTime
import poc.transaction.TransactionFlow
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.Obligacion
import poc.model.sujeto.AggregateSujeto

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

class TransactionFlowSpec extends DocsSpecBase(KafkaPorts.ScalaTransactionsExamples) {

  override def sleepAfterProduce: FiniteDuration = 10.seconds

  "TransactionFlow" should "work" in assertAllStagesStopped {
    import scala.collection.JavaConverters._
    val config: Config = ConfigFactory.parseMap(Map("kafka.brokers" -> bootstrapServers).asJava)
    val appConfig = new AppConfig(config)
    val objeto: ActorRef = AggregateObjeto.start
    val sujeto: ActorRef = AggregateSujeto.start

    val txFlow = new TransactionFlow(appConfig)

    val flow = txFlow.controlGraph(objeto, sujeto){ objetoSuccess =>
        AggregateSujeto.UpdateObjeto("1", 1L, AggregateSujeto.Objeto("1", 200.0, DateTime.now()))
    }
    flow.run()


    val obligacion = AggregateObjeto.UpdateObligacion("1", 1L, AggregateObjeto.Obligacion("1", 100, DateTime.now()))
    val range = immutable.Seq(
      obligacion.copy(deliveryId = 1, obligacion = AggregateObjeto.Obligacion("2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 2, obligacion = AggregateObjeto.Obligacion("3", 100, DateTime.now())),
      obligacion.copy(deliveryId = 3, obligacion = AggregateObjeto.Obligacion("3", 100, DateTime.now())),
      obligacion.copy(deliveryId = 4, obligacion = AggregateObjeto.Obligacion("2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 5, obligacion = AggregateObjeto.Obligacion("1", 100, DateTime.now())),
      obligacion.copy(deliveryId = 6, obligacion = AggregateObjeto.Obligacion("1", 100, DateTime.now())),
      obligacion.copy(deliveryId = 7, obligacion = AggregateObjeto.Obligacion("2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 8, obligacion = AggregateObjeto.Obligacion("2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 9, obligacion = AggregateObjeto.Obligacion("3", 100, DateTime.now())),
      obligacion.copy(deliveryId = 10, obligacion = AggregateObjeto.Obligacion("2", 100, DateTime.now())),
    )

    def produce(topic: String, range: immutable.Seq[AggregateObjeto.UpdateObligacion]): Future[Done] =
      Source(range)
        // NOTE: If no partition is specified but a key is present a partition will be chosen
        // using a hash of the key. If neither key nor partition is present a partition
        // will be assigned in a round-robin fashion.
        .map(n => new ProducerRecord(topic, partition0, DefaultKey, n))
        .runWith(Producer.plainSink(txFlow.producerSettings))

    awaitProduce(produce(appConfig.SOURCE_TOPIC, range))
  }


}
