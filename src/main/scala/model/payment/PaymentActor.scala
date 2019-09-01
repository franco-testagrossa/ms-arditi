package model.payment

import java.time.LocalDateTime

import akka.ShardedEntity
import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import model.ddd.{GetState, _}

class PaymentActor extends PersistentActor with ActorLogging {

  import PaymentActor._

  private var state: StatePayment = StatePayment.init()

  override def receiveCommand: Receive = {
    case UpdateMovimiento(aggregateRoot, deliveryId, movimiento) =>
      val evt = MovimientoUpdated(movimiento)
      persist(evt) { e =>
        state += e
        val response = UpdateSuccess(aggregateRoot, deliveryId, movimiento)
        sender() ! response
        val logMsg = "[{}][New]"
        log.info(logMsg, response)
      }

    case GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[{}][GetState|{}]"
      log.info(logMsg, persistenceId, state.toString)

    case other =>
      val logMsg = "[{}][WrongMsg|{}]"
      log.info(logMsg, persistenceId, other.toString)
  }

  override def persistenceId: String = typeName + "-" + self.path.name

  override def receiveRecover: Receive = {
    case evt: Event =>
      state += evt
    case SnapshotOffer(_, snapshot: StatePayment) =>
      state = snapshot
  }
}

object PaymentActor extends ShardedEntity {

  val typeName = "Payment"

  def props(): Props = Props(new PaymentActor)

  final case class UpdateMovimiento(
                                     aggregateRoot: String,
                                     deliveryId: BigInt,
                                     movimiento: Movimiento
                                   ) extends Command


  final case class UpdateSuccess(aggregateRoot: String, deliveryId: BigInt, movimiento: Movimiento) extends Response

  final case class MovimientoUpdated(movimiento: Movimiento) extends Event {
    def name: String = "MovimientoUpdated"
  }

  // State
  final case class Movimiento(
                               paymentId: String,
                               productId: String,
                               personId: String,
                               money: BigDecimal
                             )

  final case class StatePayment private(
                                            balance: BigDecimal,
                                            movimientos: Set[Movimiento]
                                          ) {
    def +(event: Event): StatePayment = event match {
      case MovimientoUpdated(movimiento: Movimiento) =>
        copy(
          balance = calculateBalance(movimiento),
        )
    }

    def calculateBalance(m: Movimiento): BigDecimal = balance + m.money
  }

  object StatePayment {
    def init(): StatePayment = new StatePayment(0, Set.empty[Movimiento])
  }

}
