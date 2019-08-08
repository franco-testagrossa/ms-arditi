package poc.model.objeto

import akka.actor.{ ActorLogging, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import common.cqrs.ShardedEntity
import org.joda.time.DateTime
import poc.model.ddd._
class AggregateObjeto extends PersistentActor with ActorLogging {
  import AggregateObjeto._

  private val objetoId = self.path.name
  override def persistenceId: String = typeName + "-" + objetoId

  private var state: StateObjeto = StateObjeto.init()

  override def receiveCommand: Receive = {

    case UpdateObligacion(aggregateRoot, deliveryId, obligacion) =>
      val evt = ObligacionUpdated(obligacion)
      persist(evt) { e =>

        e match {
          case ObligacionUpdated(obligacion: Obligacion) =>

            log.error("\n\n\n\n")
            log.error("SALDO" + state.saldo.toString)
            log.error(obligacion.toString)
            log.error("sujetoReport" + state.sujetoReport(obligacion).toString)

        }
        state += e

        log.error("ESTADO RESULTANTE" + state.toString)
        log.error("\n\n\n\n")
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, obligacion, state.sujetos)
        sender() ! response
        val logMsg = "[{}][ObligacionUpdated|{}][deliveryId|{}][New]"
        log.error(logMsg, persistenceId, obligacion, deliveryId)
      }

    case AggregateObjeto.GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[{}][GetState|{}]"
      log.error(logMsg, persistenceId, state.toString)

    case other =>
      val logMsg = "[{}][WrongMsg|{}]"
      log.error(logMsg, persistenceId, other.toString)
  }

  override def receiveRecover: Receive = {
    case evt: Event =>
      log.error(s"replay event: $evt")
      state += evt
    case SnapshotOffer(_, snapshot: StateObjeto) =>
      state = snapshot
  }
}

object AggregateObjeto extends ShardedEntity {

  val typeName = "AggregateObjeto"

  val props: Props = Props[AggregateObjeto]

  final case class UpdateObligacion(
      aggregateRoot: String,
      deliveryId:    Long,
      obligacion:    Obligacion
  ) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(
      aggregateRoot: String,
      deliveryId:    Long,
      obligacion:    Obligacion,
      sujetos:       Map[String, Double]
  ) extends Response

  final case class ObligacionUpdated(obligacion: Obligacion) extends Event {
    def name: String = "ObligacionUpdated"
  }

  // State
  final case class Obligacion(
      obligacionId:    String,
      sujetoId:        String,
      saldoObligacion: Double,
      fechaUltMod:     DateTime
  )

  final case class StateObjeto private (
      saldo:        Double,
      obligaciones: Map[String, Obligacion],
      sujetos:      Map[String, Double]
  ) {
    def +(event: Event): StateObjeto = event match {
      case ObligacionUpdated(obligacion: Obligacion) =>
        copy(
          saldo        = calculateSaldo(obligacion),
          obligaciones = updateObligaciones(obligacion),
          sujetos      = sujetoReport(obligacion)
        )
    }

    def sujetoReport(obligacion: Obligacion): Map[String, Double] =
      if (sujetos contains obligacion.sujetoId)
        sujetos.map { case (suj, saldo) =>
          (suj, obligacion.saldoObligacion) // only delta
        }
      else sujetos.map { case (suj, saldo) =>
        (suj, obligacion.saldoObligacion) // only delta
      } + (obligacion.sujetoId -> calculateSaldo(obligacion))

    def calculateSaldo(o: Obligacion): Double = saldo + o.saldoObligacion // is a delta with +- sign
    def updateObligaciones(o: Obligacion): Map[String, Obligacion] = {
      val saldoDelta = o.saldoObligacion
      obligaciones.get(o.obligacionId).map { ob =>
        val oldSaldo = ob.saldoObligacion
        val newObligacion = o.copy(saldoObligacion = saldoDelta + oldSaldo)
        newObligacion
      } match {
        case Some(newObligacion) => obligaciones + (newObligacion.obligacionId -> newObligacion)
        case None => obligaciones + (o.obligacionId -> o)
      }
    }
  }
  object StateObjeto {
    def init(): StateObjeto = new StateObjeto(0, Map.empty[String, Obligacion], Map.empty[String, Double])
  }

}
