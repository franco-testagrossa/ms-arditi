package poc

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.ConsumerMessage.{PartitionOffset, TransactionalMessage}
import akka.kafka.scaladsl.Consumer.{Control, DrainingControl}
import akka.kafka.scaladsl.{Consumer, Producer, Transactional}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import akka.stream.testkit.scaladsl.TestSink
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.MainPoc.{aggregateObjeto, aggregateSujeto, appConfig}
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.model.TX
import poc.objeto.AggregateObjeto
import poc.objeto.AggregateObjeto.{GetState, StateObjeto, UpdateObligacion}
import poc.sujeto.AggregateSujeto
import poc.transaction.ActorRefFlowStage.{StreamElementIn, StreamElementOut}
import poc.transaction.{ActorRefFlowStage, TransactionFlow}
import scalaz.Functor

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class TransactionFlowSpec extends DocsSpecBase(KafkaPorts.ScalaTransactionsExamples) {

  override def sleepAfterProduce: FiniteDuration = 10.seconds

  "TransactionFlow" should "work" in assertAllStagesStopped {
    import scala.collection.JavaConverters._
    val config: Config = ConfigFactory.parseMap(Map("kafka.brokers" -> bootstrapServers).asJava)
    val appConfig = new AppConfig(config)
    val aggregateObjeto: ActorRef = AggregateObjeto.start
    val aggregateSujeto: ActorRef = AggregateSujeto.start

    val objeto =
      ActorRefFlowStage.fromActor[
        AggregateObjeto.UpdateObligacion,
        AggregateObjeto.UpdateSuccess
      ](aggregateObjeto)

    val sujeto =
      ActorRefFlowStage.fromActor[
        AggregateSujeto.UpdateObjeto,
        AggregateSujeto.UpdateSuccess
      ](aggregateObjeto)

    val txFlow = new TransactionFlow(appConfig)

    import TX._
    val flow = txFlow.controlGraph(objeto, sujeto){ objetoSuccess =>
        AggregateSujeto.UpdateObjeto("1", 1L, "1", 200.0)
    }
    flow.run()
  }
}
