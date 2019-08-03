package poc

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import poc.objeto.AggregateObjeto

import scala.concurrent.duration._
import poc.model._
import poc.objeto.AggregateObjeto.{UpdateObligacion, UpdateSuccess}
import poc.sujeto.AggregateSujeto
import poc.transaction.ActorRefFlowStage.StreamElementIn
import poc.transaction.{ActorRefFlowStage, TransactionFlow}
import scalaz.Functor

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

  private val appConfig = new AppConfig(config)

  // Start Up TransactionFlow
  private val objeto =
    ActorRefFlowStage.fromActor[
      TX[AggregateObjeto.UpdateObligacion],
      TX[AggregateObjeto.UpdateSuccess]
    ](aggregateObjeto)

  private val sujeto =
    ActorRefFlowStage.fromActor[
      TX[AggregateSujeto.UpdateObjeto],
      TX[AggregateSujeto.UpdateSuccess]
    ](aggregateSujeto)

  val txFlow = new TransactionFlow(appConfig)

  import TX._
  private val flow = txFlow.controlGraph(objeto, sujeto) { objetoSuccess =>
    implicitly[Functor[TX]].map(objetoSuccess) { updateSuccess =>
      AggregateSujeto.UpdateObjeto("1", 1L, "1", 200.0)
    }
  }
  flow.run()

  // Start Up Rest API
  startRest(aggregateObjeto, aggregateSujeto, appConfig)

  private def startRest(objetoService: ActorRef, sujetoService: ActorRef, config: AppConfig): Unit = {
    import config._

    implicit val timeout: Timeout = Timeout(10 seconds)
    val host = API_HOST
    val port = API_PORT

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