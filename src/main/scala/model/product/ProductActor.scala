package model.product

import java.time.LocalDateTime

import akka.ShardedEntity
import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import model.ddd.{ GetState, _ }

class ProductActor extends PersistentActor with ActorLogging {

  import ProductActor._

  private var state: StateProduct = StateProduct.init()

  override def receiveCommand: Receive = {

    case UpdatePayment(aggregateRoot, deliveryId, payment) =>
      val evt = PaymentUpdated(payment)
      persist(evt) { e =>
        state += e
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, payment, state.persons)
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
    case SnapshotOffer(_, snapshot: StateProduct) =>
      state = snapshot
  }
}

object ProductActor extends ShardedEntity {

  val typeName = "Product"

  def props(): Props = Props(new ProductActor)

  final case class UpdatePayment(
    aggregateRoot: String,
    deliveryId:    BigInt,
    payment:       Payment
  ) extends Command

  final case class UpdateSuccess(
    aggregateRoot: String,
    deliveryId:    BigInt,
    payment:       Payment,
    persons:       Map[String, BigDecimal]
  ) extends Response

  final case class PaymentUpdated(payment: Payment) extends Event {
    def name: String = "PaymentUpdated"
  }

  // State
  final case class Payment(
    paymentId:      String,
    personId:       String,
    balancePayment: BigDecimal
  )

  final case class StateProduct private (
    balance:   BigDecimal,
    paymentes: Map[String, Payment],
    persons:   Map[String, BigDecimal]
  ) {
    def +(event: Event): StateProduct = event match {
      case PaymentUpdated(payment: Payment) =>
        copy(
          balance   = calculateBalance(payment),
          paymentes = updatePaymentes(payment),
          persons   = personReport(payment)
        )
    }

    def personReport(payment: Payment): Map[String, BigDecimal] =
      if (persons contains payment.personId)
        persons.map {
          case (suj, balance) =>
            (suj, payment.balancePayment) // only delta
        }
      else persons.map {
        case (suj, balance) =>
          (suj, payment.balancePayment) // only delta
      } + (payment.personId -> calculateBalance(payment))

    def calculateBalance(o: Payment): BigDecimal = balance + o.balancePayment // is a delta with +- sign
    def updatePaymentes(o: Payment): Map[String, Payment] = {
      val balanceDelta = o.balancePayment
      paymentes.get(o.paymentId) map { ob =>
        val oldBalance = ob.balancePayment
        val newPayment = o.copy(balancePayment = balanceDelta + oldBalance)
        newPayment
      } match {
        case Some(newPayment) => paymentes + (newPayment.paymentId -> newPayment)
        case None => paymentes + (o.paymentId -> o)
      }
    }
  }

  object StateProduct {
    def init(): StateProduct = new StateProduct(0, Map.empty[String, Payment], Map.empty[String, BigDecimal])
  }

}
