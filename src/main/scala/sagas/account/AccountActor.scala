package sagas.account

import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import sagas.domain.Account

object AccountActor {

  sealed trait Command
  final case class FreezeMoney(deliveryId: Long, transactionId: Long, to: Account, amount: Long) extends Command
  final case class AddMoney(deliveryId: Long, transaction: Long, from: Account, amount: Long) extends Command
  final case class FinishTransaction(deliveryId: Long, transactionId: Long) extends Command
  final case class UnfreezeMoney(deliveryId: Long, transactionId: Long) extends Command

  sealed trait Response
  final case class ConfirmMoneyFrozenSucc(deliveryId: Long) extends Response
  final case class ConfirmMoneyFrozenFail(deliveryId: Long, reason: String) extends Response
  final case class ConfirmMoneyAddedSucc(deliveryId: Long) extends Response
  final case class ConfirmMoneyAddedFail(deliveryId: Long, reason: String) extends Response
  final case class ConfirmTransactionFinished(deliveryId: Long) extends Response
  final case class ConfirmMoneyUnfrozen(deliveryId: Long) extends Response

  sealed trait Event
  final case class MoneyFrozen(transactionId: Long, to: Account, amount: Long) extends Event
  final case class MoneyAdded(transactionId: Long, from: Account, amount: Long) extends Event
  final case class TransactionFinished(transactionId: Long) extends Event
  final case class MoneyUnfrozen(transactionId: Long) extends Event

  sealed trait UtilCommand extends Command
  final case object GetState extends UtilCommand
  final case class IncreaseAmount(amount: Long) extends UtilCommand

  sealed trait UtilResponse extends Response
  final case object ConfirmAmountIncreased extends UtilResponse

  sealed trait UtilEvent extends Event
  final case class AmountIncreased(amount: Long) extends UtilEvent

  def props(active: Boolean, balance: Long): Props = Props(classOf[AccountActor], active, balance)

  // Aggregate State
  final case class Transaction(transactionId: Long, from: Account, to: Account, amount: Long)
  final case class AccountState(
      account:              String,
      active:               Boolean,
      balance:              Long,
      finishedTransactions: Map[Long, Transaction],
      inFlightTransaction:  Map[Long, Transaction]
  ) {

    def updated(event: Event): AccountState = event match {
      case AmountIncreased(amount) =>
        copy(balance = balance + amount)

      case MoneyFrozen(id, to, amount) =>
        val transaction = Transaction(id, account, to, amount)
        copy(balance             = balance - amount, inFlightTransaction = inFlightTransaction + (id -> transaction))

      case MoneyAdded(id, from, amount) =>
        val transaction = Transaction(id, from, account, amount)
        copy(balance              = balance + amount, finishedTransactions = finishedTransactions + (id -> transaction))

      case TransactionFinished(id) =>
        val transaction = inFlightTransaction(id)
        copy(
          finishedTransactions = finishedTransactions + (id -> transaction),
          inFlightTransaction  = inFlightTransaction - id
        )

      case MoneyUnfrozen(id) =>
        val transaction = inFlightTransaction(id)
        copy(balance             = balance + transaction.amount, inFlightTransaction = inFlightTransaction - id)

    }
  }
}

class AccountActor(_active: Boolean, _balance: Long) extends PersistentActor with ActorLogging {

  import AccountActor._

  var state = AccountState(
    account = _account,
    active  = _active,
    balance = _balance,
    Map.empty, Map.empty
  )

  def _account: String = self.path.name

  override val persistenceId: String = getClass.getSimpleName + "-" + _account

  def updateState(event: Event): Unit =
    state = state.updated(event)

  val receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_, snapshot: AccountState) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case msg @ GetState =>
      log.info(s"received $msg ")
      val replyTo = sender()
      replyTo ! state

    case msg @ IncreaseAmount(amount) =>
      log.info(s"received $msg ")
      val replyTo = sender()
      persist(AmountIncreased(amount)) { e =>
        updateState(e)
        replyTo ! ConfirmAmountIncreased
      }

    case msg @ FreezeMoney(deliveryId, transactionId, to, amount) =>
      log.info(s"received $msg ")
      val replyTo = sender()
      if (state.finishedTransactions.contains(transactionId)
        || state.inFlightTransaction.contains(transactionId)) {
        replyTo ! ConfirmMoneyFrozenSucc(deliveryId)
      }
      else if (state.balance < amount) {
        replyTo ! ConfirmMoneyFrozenFail(
          deliveryId,
          s"insufficient balance, ${_account} have ${state.balance}, which is less than $amount"
        )
      }
      else if (!state.active) {
        replyTo ! ConfirmMoneyFrozenFail(deliveryId, s"account ${_account} is not active")
      }
      else {
        persist(MoneyFrozen(transactionId, to, amount)) { e =>
          updateState(e)
          replyTo ! ConfirmMoneyFrozenSucc(deliveryId)
        }
      }

    case msg @ AddMoney(deliveryId, transactionId, from, amount) =>
      log.info(s"received $msg ")
      val replyTo = sender()
      if (state.finishedTransactions.contains(transactionId)) {
        replyTo ! ConfirmMoneyAddedSucc(deliveryId)
      }
      else if (!state.active) {
        replyTo ! ConfirmMoneyAddedFail(deliveryId, s"account ${_account} is not active")
      }
      else {
        persist(MoneyAdded(transactionId, from, amount)) { e =>
          updateState(e)
          replyTo ! ConfirmMoneyAddedSucc(deliveryId)
        }
      }

    case msg @ FinishTransaction(deliveryId, transactionId) =>
      log.info(s"received $msg ")
      val replyTo = sender()
      if (state.finishedTransactions.contains(transactionId)) {
        replyTo ! ConfirmTransactionFinished(deliveryId)
      }
      else {
        persist(TransactionFinished(transactionId)) { e =>
          updateState(e)
          replyTo ! ConfirmTransactionFinished(deliveryId)
        }
      }

    case msg @ UnfreezeMoney(deliveryId, transactionId) =>
      log.info(s"received $msg ")
      val replyTo = sender()
      if (!state.inFlightTransaction.contains(transactionId)) {
        replyTo ! ConfirmMoneyUnfrozen(deliveryId)
      }
      else {
        persist(MoneyUnfrozen(transactionId)) { e =>
          updateState(e)
          replyTo ! ConfirmMoneyUnfrozen(deliveryId)
        }
      }

    case "print" =>
      log.info(s"account: ${_account} state is ${state.toString}")
  }

}
