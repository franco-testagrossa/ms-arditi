package runner
import scala.Console._
import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.PoisonPill

object ActorRunner extends App {
  val system = ActorSystem("ms-arditi")
  val actorRunner = system.actorOf(Props[ActorRunner], "actor-runner")

  actorRunner ! "msg"
  actorRunner ! "hey"
  actorRunner ! PoisonPill

  Thread.sleep(2000)
  system.terminate()
}

class ActorRunner extends Actor with ActorLogging {
  import ActorRunner._

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