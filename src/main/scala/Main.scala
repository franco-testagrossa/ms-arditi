import akka.actor
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout
import api.ApiRoutes
import com.typesafe.config.ConfigFactory
import config.AppConfig
import model.payment.PaymentActor
import model.person.PersonActor
import model.product.ProductActor
import transaction.TransactionFlow

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor }

object Main extends App {

  val customConf =
    ConfigFactory.parseString(s"""
         akka {
           cluster {
             seed-nodes = ["akka://ClusterArditi@127.0.0.1:2551"]
           }
         }
        """)
  val coccnfig = customConf.withFallback(ConfigFactory.load()).resolve()

  implicit val system: actor.ActorSystem = ActorSystem("ClusterArditi", coccnfig)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Start Up Akka Cluster
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  val payment: ActorRef = PaymentActor.start
  val product: ActorRef = ProductActor.start
  val person: ActorRef = PersonActor.start
  lazy val config = ConfigFactory.load()
  val appConfig = new AppConfig(config)

  // Start Up Rest API
  startApi(Seq(payment, product, person), appConfig)

  val txFlow = new TransactionFlow(appConfig)

  implicit val timeout: Timeout = Timeout(10 seconds)

  import kafka.DefaultSettings._

  val flow = txFlow.controlGraph(payment, product, person)

  println(s"Node ${appConfig.NODEID} is listening at http://${appConfig.API_HOST}:${appConfig.API_PORT}")
  Await.result(system.whenTerminated, Duration.Inf)

  private def startApi(actors: Seq[ActorRef], config: AppConfig) = {
    import config._

    implicit val timeout: Timeout = Timeout(10 seconds)
    val host = API_HOST
    val port = API_PORT

    val httpClient: ApiRoutes = new ApiRoutes(actors)
    val route = httpClient.routes

    val _ = Http().bindAndHandle(route, host, port)
      .map { binding =>
        println(s"Starting models observer on port ${binding.localAddress}")
      }
      .recover {
        case ex =>
          println(s"Models observer could not bind to $host:$port/ ${ex.getMessage}")
      }
  }
}
