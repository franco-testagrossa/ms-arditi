package poc.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives.{ complete, get, path, _ }
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.UpdateObligacion
import poc.model.sujeto.AggregateSujeto
import poc.model.sujeto.AggregateSujeto.{ Objeto, UpdateObjeto }

class ApiRoutes(objetoService: ActorRef, sujetoService: ActorRef)
  (implicit timeout: Timeout, ec: ExecutionContext) {

  def routes: Route = concat(

    (get & pathPrefix("state")) {
      //      instancesRoutes ~
      objetoRoutes ~ sujetoRoutes
    },

    (post & pathPrefix("state")) {
      //      instancesRoutes ~
      objetoRoutes ~ sujetoRoutes
    }
  )

  def objetoRoutes: Route =
    path("objeto" / LongNumber) { id =>
      concat(
        get {
          complete{
            (objetoService ? AggregateObjeto.GetState(id.toString))
              .mapTo[AggregateObjeto.StateObjeto]
              .map { stateObjeto =>
                s"success : $stateObjeto"
              }
              .recover { case e: Exception => s"Exception : $e" }
          }
        },
        post {
          complete{
            (objetoService ? UpdateObligacion("1", 1, AggregateObjeto.Obligacion("1", "2", 2000.0, DateTime.now())))
              .mapTo[AggregateObjeto.UpdateSuccess]
              .map { stateObjeto =>
                s"success : $stateObjeto"
              }
              .recover { case e: Exception => s"Exception : $e" }
          }
        }
      )

    }

  def sujetoRoutes: Route =
    path("sujeto" / LongNumber) { id =>
      concat(
        get {
          complete {
            (sujetoService ? AggregateSujeto.GetState(id.toString))
              .mapTo[AggregateSujeto.StateSujeto]
              .map { stateSujeto =>
                s"success : $stateSujeto"
              }
              .recover { case e: Exception => s"Exception : $e" }
          }
        },
        post {
          complete{
            (sujetoService ?
              AggregateSujeto.UpdateObjeto(
                aggregateRoot = "1",
                deliveryId    = 1L,
                objeto        = Objeto(
                  objetoId    = "1",
                  sujetoId    = "1",
                  saldoObjeto = 2000.0,
                  fechaUltMod = DateTime.now
                )
              ))
              .mapTo[AggregateSujeto.UpdateSuccess]
              .map { stateSujeto =>
                s"success : $stateSujeto"
              }
              .recover { case e: Exception => s"Exception : $e" }
          }
        }
      )
    }
}
