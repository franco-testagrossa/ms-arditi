package poc.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import poc.model.objeto.AggregateObjeto
import poc.model.sujeto.AggregateSujeto


class ApiRoutes(objetoService: ActorRef, sujetoService: ActorRef)
               (implicit timeout: Timeout, ec: ExecutionContext) {

  def routes: Route =
    (get & pathPrefix("state")) {
      //      instancesRoutes ~
      objetoRoutes ~ sujetoRoutes
    }

  def objetoRoutes: Route =
    path("objeto"/ LongNumber) { id =>
      get {
        complete{
          (objetoService ? AggregateObjeto.GetState(id.toString))
            .mapTo[AggregateObjeto.StateObjeto]
            .map { stateObjeto =>
              s"success : $stateObjeto"
            }
            .recover { case e: Exception => s"Exception : $e" }
        }
      }
    }

  def sujetoRoutes: Route =
    path("sujeto" / LongNumber) { id =>
      get {
        complete {
          (sujetoService ? AggregateObjeto.GetState(id.toString))
            .mapTo[AggregateSujeto.StateSujeto]
            .map { stateSujeto =>
              s"success : $stateSujeto"
            }
            .recover { case e: Exception => s"Exception : $e" }
        }
      }
    }
}
