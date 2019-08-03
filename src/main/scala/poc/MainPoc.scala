package poc

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.kafka.ConsumerMessage.TransactionalMessage
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.Transactional
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.objeto.AggregateObjeto

import scala.concurrent.duration._
import poc.model._

object MainPoc {
  private lazy val config = ConfigFactory.load()
  private implicit val system = ActorSystem("ClusterArditi")
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  // Start Up Akka Cluster
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  private val aggregateObjeto: ActorRef = AggregateObjeto.start(system)
  // private val aggregateSujeto: ActorRef = AggregateSujeto.start(system)

  // Control Flow Settings
  private val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new KafkaDeserializer[ModelRequest])
      .withBootstrapServers("config.KAFKA_BROKER") // TODO
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId("DATA_GROUP") // TODO

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new KafkaSerializer[ModelResponse])
      .withBootstrapServers("bootstrapServers") // TODO

  type TX[A] = TransactionalMessage[String, A]
  private val model = new ActorRefFlowStage[TX[ModelRequest], TX[ModelResponse]](aggregateObjeto)


  // Run Control Flow
  private val sourceTopic = Set("sourceTopic") // TODO
  private val sinkTopic = "sinkTopic" // TODO
  private def transactionalId: String = java.util.UUID.randomUUID().toString
  private val control =
    Transactional
      .source(consumerSettings, Subscriptions.topics(sourceTopic))
      .map { msg: TX[ModelRequest] => msg }
      .via(model)
      .map { msg: TX[ModelResponse] =>
        ProducerMessage.single(
          new ProducerRecord(sinkTopic, msg.record.key, msg.record.value), msg.partitionOffset
        )
      }
      .toMat(Transactional.sink(producerSettings, transactionalId))(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()


  startRest(aggregateObjeto, config)

  // TODO
  private def startRest(service: ActorRef, config: Config): Unit = {
    implicit val timeout = Timeout(10 seconds)
    // // complete(config.getString("application.api.hello-message"))
    val host = "0.0.0.0"  // config.getString("application.api.host")
    val port = 1 // MODEL_SERVER_PORT - config.getInt("application.api.port")
    val routes: Route = get {
      path("stats") {
        complete("OK")
      }
    }

    val _ = Http().bindAndHandle(routes, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port/ ${ex.getMessage}")
    }
    ()
  }
}