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

    // AggregateObjeto.UpdateSuccess => AggregateSujeto.UpdateObjeto
    val flow = txFlow.controlGraph(objeto, sujeto){ objetoSuccess =>
        val sujetoId = objetoSuccess.obligacion.sujetoId
        val deliveryId = objetoSuccess.deliveryId
        val objetoId = objetoSuccess.aggregateRoot
        val lastUpdated = objetoSuccess.obligacion.fechaUltMod
        val saldo = objetoSuccess.obligacion.saldoObligacion
        AggregateSujeto.UpdateObjeto(sujetoId, deliveryId, saldo, objetoId, DateTime.now())
    }
    flow.run()


    val obligacion = AggregateObjeto.UpdateObligacion("1", 1L, AggregateObjeto.Obligacion("1", "666", 100, DateTime.now()))
    val range = immutable.Seq(
      obligacion.copy(deliveryId = 1L, obligacion = AggregateObjeto.Obligacion("2", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 2L, obligacion = AggregateObjeto.Obligacion("3", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 3L, obligacion = AggregateObjeto.Obligacion("3", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 4L, obligacion = AggregateObjeto.Obligacion("2", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 5L, obligacion = AggregateObjeto.Obligacion("1", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 6L, obligacion = AggregateObjeto.Obligacion("1", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 7L, obligacion = AggregateObjeto.Obligacion("2", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 8L, obligacion = AggregateObjeto.Obligacion("2", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 9L, obligacion = AggregateObjeto.Obligacion("3", "666", 100, DateTime.now())),
      obligacion.copy(deliveryId = 10L, obligacion = AggregateObjeto.Obligacion("2", "666", 100, DateTime.now())),
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
