package poc

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import poc.api.ApiRoutes
import poc.transaction.TransactionFlow
import poc.model.objeto.AggregateObjeto
import poc.model.sujeto.AggregateSujeto

object MainPoc extends App {
  private lazy val config = ConfigFactory.load()
  private implicit val system: ActorSystem = ActorSystem("ClusterArditi")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Start Up Akka Cluster
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  private val objeto: ActorRef = AggregateObjeto.start
  private val sujeto: ActorRef = AggregateSujeto.start

  private val appConfig = new AppConfig(config)
  val txFlow = new TransactionFlow(appConfig)

  import akka.util.Timeout
  import scala.concurrent.duration._
  implicit val timeout: Timeout = Timeout(10 seconds)
  // private val flow = txFlow.controlGraph(objeto, sujeto) { objetoSuccess =>
  //     val sujetoId     = objetoSuccess.obligacion.sujetoId
  //     val deliveryId   = objetoSuccess.deliveryId
  //     val objetoId     = objetoSuccess.aggregateRoot
  //     val lastUpdated  = objetoSuccess.obligacion.fechaUltMod
  //     val saldo        = objetoSuccess.obligacion.saldoObligacion
  //     AggregateSujeto.UpdateObjeto(sujetoId, deliveryId, saldo, objetoId, DateTime.now())
  // }
  // flow.run()

  // Start Up Rest API
  startApi(objeto, sujeto, appConfig)

  private def startApi(objetoService: ActorRef, sujetoService: ActorRef, config: AppConfig): Unit = {
    import config._

    implicit val timeout: Timeout = Timeout(10 seconds)
    val host = API_HOST
    val port = API_PORT + 1

    val httpClient = new ApiRoutes(objetoService, sujetoService)
    val route = httpClient.routes

    val _ = Http().bindAndHandle(route, host, port) map
      { binding => println(s"Starting models observer on port ${binding.localAddress}") } recover {
      case ex =>
        println(s"Models observer could not bind to $host:$port/ ${ex.getMessage}")
    }
    ()
  }
}