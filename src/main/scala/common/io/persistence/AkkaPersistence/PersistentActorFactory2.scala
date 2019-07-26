import PAImpl.Run
import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.persistence.{ PersistentActor, SnapshotOffer }

import scala.concurrent.Future

object PAImplClient extends App {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  val as = ActorSystem("ClusterArditi")
  val paImpl: ActorRef = PAImpl.start(as) // Factory

  implicit val timeout = Timeout(3 seconds)
  // https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html
  val eventualRunned: Future[PAImpl.Runned] =
    (paImpl ? Run("1")).mapTo[PAImpl.Runned]

}

// type Receive = PartialFunction[Any, Unit]
class PAImpl extends PersistentActor {
  import PAImpl._

  override def persistenceId: String = typeName + "-" + self.path.name

  //var interpreter: Any = ???
  var state: String = "" // State.init
  override def receiveCommand: Receive = {
    case cmd: PAImpl.Run =>
      /*
      val machine => interpreter.interpret(state)
      val newMachine: PersistentEffect[State] = machine.run(cmd)
      newMachine.flatMap { newSt =>
        state += evt.id // state is updated
        sender() ! Some(state) // a Response (Some) is sent back to client
      }
       */

      if (cmd.id > "0") {
        persist(Runned(cmd.id)) { evt =>
          state += evt.id // state is updated
          sender() ! Some(state) // a Response (Some) is sent back to client
        }
      }
    case _ => println("HEY!")
  }

  val receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: String) => state = snapshot
    case evt => state += evt
  }
}

object PAImpl {
  val typeName = "PAImpl"

  def props(): Props = Props[PAImpl]

  // Protocolo de PAImpl
  trait Command { def id: String }
  case class Run(id: String) extends Command

  trait Event
  case class Runned(id: String) extends Event

  // Factory Method for PAImpl
  def start(system: ActorSystem) = ClusterSharding(system).start(
    typeName = typeName,
    entityProps = Props[PAImpl],
    settings = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId = extractShardId(1))

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.id, cmd)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case cmd: Command => (cmd.id.toLong % numberOfShards).toString
  }
}
