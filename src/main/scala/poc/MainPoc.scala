package poc

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import poc.objeto.AggregateObjeto

import scala.concurrent.duration._
import poc.model._
import poc.sujeto.AggregateSujeto
import poc.transaction.{ActorRefFlowStage, TransactionFlow}

import scala.concurrent.ExecutionContextExecutor

object MainPoc {
  private lazy val config = ConfigFactory.load()
  private implicit val system: ActorSystem = ActorSystem("ClusterArditi")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Start Up Akka Cluster
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  private val aggregateObjeto: ActorRef = AggregateObjeto.start
  private val aggregateSujeto: ActorRef = AggregateSujeto.start

  // Start Up TransactionFlow
  private val objeto = actorRefStage[TX[ModelRequest], TX[ModelResponse]](aggregateObjeto)
  private val sujeto = actorRefStage[TX[ModelResponse], TX[ModelResponse]](aggregateSujeto)
  TransactionFlow.controlGraph(objeto, sujeto)(identity).run()

  // Start Up Rest API
  startRest(aggregateObjeto, aggregateSujeto, config)

  private def actorRefStage[A,B](actorRef: ActorRef): ActorRefFlowStage[TX[A], TX[B]] =
    new ActorRefFlowStage[TX[A], TX[B]](actorRef)
  private def startRest(objetoService: ActorRef, sujetoService: ActorRef, config: Config): Unit = {
    // TODO
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