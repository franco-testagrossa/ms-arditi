package poc

import scala.concurrent.{Await, ExecutionContextExecutor}
import poc.api.ApiRoutes
import poc.transaction.TransactionFlow
import poc.model.objeto.AggregateObjeto
import poc.model.sujeto.AggregateSujeto
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import poc.model.sujeto.AggregateSujeto.Objeto

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MainPoc extends App {

  import akka.util.Timeout
  import scala.concurrent.duration._
  implicit val timeout: Timeout = Timeout(10 seconds)

  implicit val system: ActorSystem = ActorSystem("ClusterArditi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val config: Config = ConfigFactory.load()
  val address = config.getString("http.ip")
  val port = config.getInt("http.port")
  val nodeId = config.getString("clustering.ip")

  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val objeto: ActorRef = AggregateObjeto.start
  private val sujeto: ActorRef = AggregateSujeto.start

  private val appConfig = new AppConfig(ConfigFactory.load())
  val txFlow = new TransactionFlow(appConfig)

  import scala.concurrent.duration._
  val httpClient: ApiRoutes = new ApiRoutes(objeto, sujeto)
  lazy val routes = httpClient.routes

  val flow = txFlow.controlGraph(objeto, sujeto) { objetoSuccess =>
    val sujetoId = objetoSuccess.obligacion.sujetoId
    val deliveryId = objetoSuccess.deliveryId
    val objetoId = objetoSuccess.aggregateRoot
    val lastUpdated = objetoSuccess.obligacion.fechaUltMod
    val saldo = objetoSuccess.obligacion.saldoObligacion
    AggregateSujeto.UpdateObjeto(

      aggregateRoot = sujetoId,
      deliveryId = deliveryId,
      objeto = Objeto(
        objetoId = objetoId,
        sujetoId=        sujetoId,
        saldoObjeto= saldo,
        fechaUltMod=     DateTime.now
      )
    )
  }
  flow.run()

  Http().bindAndHandle(routes, address, port)
  println(s"Node $nodeId is listening at http://$address:$port")

  Await.result(system.whenTerminated, Duration.Inf)

}
