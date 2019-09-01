package api

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import model.ddd.GetState
import scala.concurrent.ExecutionContext

class ApiRoutes(actors: Seq[ActorRef])
  (implicit timeout: Timeout, ec: ExecutionContext, mat: Materializer) {

  def routes: Route =
    (get & pathPrefix("state")) {
      actors.map(actorRoutes).reduceLeft((a, b) => a ~ b)
    } ~ get {
      path("health") {
        complete {
          HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>service is healthy</h1>")
        }
      }
    }

  println("HTTP routes: ")
  actors.foreach(a => println(s"http://0.0.0.0:8080/state/${a.path.name}/1 "))

  def actorRoutes(actorRef: ActorRef): Route =
    path(actorRef.path.name / Segment) { id =>
      concat(
        get {
          complete {
            (actorRef ? GetState(id))
              .map { result =>
                s"success : $result"
              }
              .recover { case e: Exception => s"Exception : $e" }
          }
        }
      )
    }
}
