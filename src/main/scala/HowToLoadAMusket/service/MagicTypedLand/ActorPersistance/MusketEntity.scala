package HowToLoadAMusket.service.MagicTypedLand.ActorPersistance

import HowToLoadAMusket.api.MusketApi
import HowToLoadAMusket.domain.MusketState._
import HowToLoadAMusket.domain._
import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.Tagged
import common.cqrs.Settings

import scala.concurrent.ExecutionContext.Implicits.global
import scala.math.abs

// PIPE PATTERN       future.pipeTo(sender()) // use the pipe pattern
object MusketEntity {


  object Commands {

    sealed trait Command

    final case object Prepare extends Command

    final case object Aim extends Command

    final case object Shoot extends Command

  }
  object Events {
    sealed trait Event

    final case object PreparedEvent extends Event
    final case object AimedEvent extends Event
    final case object ShotEvent extends Event

    final case class SwitchCreated(n: Int) extends Event
  }


  def props: Props =
    Props(new MusketEntity)
}

class MusketEntity extends PersistentActor with ActorLogging with MusketApi {

  import MusketEntity.Commands._
  import MusketEntity.Events._

  private val settings = Settings(context.system)

  override def persistenceId: String = "MusketEntity|" + context.self.path.name

  private def eventTag =
    s"${settings.eventProcessorSettings.tagPrefix}${abs(persistenceId.hashCode % settings.eventProcessorSettings.parallelism)}"

  private var musketStatus: MusketState = Unprepared()

  override def receiveRecover: Receive = {
    case event: Event => applyEvent(event)
  }

  def persistEvent(event: Event) = {
    // The tagged events are consumed by the `EventProcessor`
    val taggedEvent = Tagged(event, Set(s"$eventTag"))
    persist(taggedEvent) { _ =>
      applyEvent(event)
      log.info("persisted {}", taggedEvent)
    }
  }

  override def receiveCommand: Receive = {

    case Prepare =>
      musketStatus match {
        case musket @ Unprepared() =>
          prepare(musket).map(preparedMusket => persistEvent(PreparedEvent))

        case _ => log.info("Prepare", musketStatus)
      }

    case Aim =>
      musketStatus match {
        case musket @ Prepared() =>
          aim(musket).map(aimedMusket => persistEvent(AimedEvent))

        case _ => log.info("Aim")
      }

    case Shoot =>
      musketStatus match {
        case musket @ Aimed() =>
          shoot(musket).map(shotMusket => persistEvent(ShotEvent))
        case _ => log.info("Shoot")

      }

  }

  private def applyEvent(event: Event): Unit = {
    println(event)
    event match {
      case SwitchCreated(n) =>
        musketStatus = Prepared()
      case PreparedEvent =>
        musketStatus = Prepared()
      case AimedEvent =>
        musketStatus = Aimed()
      case ShotEvent =>
        musketStatus = Shot()

    }
  }

}
