package poc.model.obligacion

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}
import common.cqrs.ShardedEntity
import org.joda.time.DateTime
import poc.model.ddd._

class AggregateObligacion extends PersistentActor with ActorLogging {
  import AggregateObligacion._

  private val obligacionId = self.path.name
  override def persistenceId: String = typeName + "-" + obligacionId

  private var state: StateObligacion = StateObligacion.init()

  override def receiveCommand: Receive = {
    case UpdateMovimiento(aggregateRoot, deliveryId, movimiento) =>
      val evt = MovimientoUpdated(movimiento)
      persist(evt) { e =>
        state += e
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, movimiento)
        sender() ! response
        val logMsg = "[{}][MovimientoUpdated|{}][deliveryId|{}][New]"
        log.info(logMsg, persistenceId, movimiento, deliveryId)
      }

    case AggregateObligacion.GetState(_) =>
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
      log.info(s"replay event: $evt")
      state += evt
    case SnapshotOffer(_, snapshot: StateObligacion) =>
      state = snapshot
  }
}

object AggregateObligacion extends ShardedEntity {

  val typeName = "AggregateObligacion"

  val props: Props = Props[AggregateObligacion]

  final case class UpdateMovimiento(
      aggregateRoot: String,
      deliveryId:    Long,
      movimiento:    Movimiento
  ) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(aggregateRoot: String, deliveryId: Long, movimiento: Movimiento) extends Response

  final case class MovimientoUpdated(movimiento: Movimiento) extends Event {
    def name: String = "MovimientoUpdated"
  }

  // State
  final case class Movimiento(
      evoId: String,
      objetoId: String,
      sujetoId: String,
      importe: Double,
      tipoMovimiento: Char, // D | C
      fechaUltMod: DateTime
  )

  final case class StateObligacion private (
      saldo:        Double,
      movimientos: Set[Movimiento]
  ) {
    def +(event: Event): StateObligacion = event match {
      case MovimientoUpdated(movimiento: Movimiento) =>
        copy(
          saldo        = calculateSaldo(movimiento),
          // movimientos  = updateMovimientos(movimiento)
        )
    }

    def calculateSaldo(m: Movimiento): Double = m.tipoMovimiento match {
      case 'C' => saldo + m.importe
      case 'D' => saldo - m.importe
    }
    // def updateMovimientos(m: Movimiento): Set[Movimiento] = movimientos + m
  }
  object StateObligacion {
    def init(): StateObligacion = new StateObligacion(0, Set.empty[Movimiento])
  }

}
