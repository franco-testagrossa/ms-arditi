package runner
import scala.Console._
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.PoisonPill

class ActorRunner extends Actor with ActorLogging {
  def cyan(txt: String): Unit = log.info(Console.CYAN + txt + Console.RESET)

  override def preStart(): Unit =
    cyan("preStart executed on ActorRunner")

  override def postStop(): Unit =
    cyan("postStop executed on ActorRunner")

  def receive: Actor.Receive = {
    case msg =>
      log.info(Console.YELLOW + s"msg ${
        Console.RED + msg + Console.YELLOW
      } received on ActorRunner" + Console.RESET)
      sender ! self.path.toString
  }
}
