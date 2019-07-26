package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter

import domainDrivenDesign.Abstractions.{ Event, EventStore }
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.Account
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.event.{ Credited, Debited, Opened }
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.algebra.AccountRules
import scalaz.{ Scalaz, \/, ~> }
import Scalaz._
import org.joda.time.DateTime
import scalaz.concurrent.Task

trait AccountRulesV1 extends AccountRules with EventStore[String] {

  import domainDrivenDesign.boundedContexts.bank.modules.account.interpreter.snapshot.AccountSnapshot._

  //val eventStore = InMemoryEventStore.apply
  val step: Event ~> Task = new (Event ~> Task) {
    override def apply[A](action: Event[A]): Task[A] = handleCommand(action)
  }

  /*val step: Event ~> Task = new (Event ~> Task) {
    override def apply(action: Free[Event, Account]): Task[Account] = handleCommand(action.)
  }*/

  def closed(a: Account): Error \/ Account =
    if (a.dateOfClosing isDefined) new Error(s"Account ${a.id} is closed").left
    else a.right

  def beforeOpeningDate(a: Account, cd: Option[DateTime]): Error \/ Account =
    if (a.dateOfOpening isBefore cd.getOrElse(DateTime.now()))
      new Error(s"Cannot close at a date earlier than opening date ${a.dateOfOpening}").left
    else a.right

  def sufficientFundsToDebit(a: Account, amount: BigDecimal): Error \/ Account =
    if (a.balance.amount < amount) new Error(s"insufficient fund to debit $amount from ${a.id}").left
    else a.right

  def validateClose(id: String, cd: Option[DateTime]) = for {
    l <- events(id)
    s <- snapshot(l)
    a <- closed(s(id))
    _ <- beforeOpeningDate(a, cd)
  } yield s

  def validateDebit(id: String, amount: BigDecimal) =
    for {
      l <- events(id)
      s <- snapshot(l)
      a <- closed(s(id))
      _ <- sufficientFundsToDebit(a, amount)
    } yield s

  def validateCredit(id: String) = for {
    l <- events(id)
    s <- snapshot(l)
    _ <- closed(s(id))
  } yield s

  def validateOpen(id: String) = {
    val events = get(id)
    if (events nonEmpty) s"Account with id = $id already exists".left
    else id.right
  }

  def handleCommand[A](e: Event[A]): Task[A] = e match {

    case o @ Opened(id, name, odate, _) => Task {
      validateOpen(id).fold[Account](
        err => throw new RuntimeException(err),
        _ => {
          val a = Account(id, name, odate.get)
          put(id, o)
          a
        })
    }

    case d @ Debited(no, amount, _) => Task {
      validateDebit(no, amount).fold[Account](
        err => throw new RuntimeException(err),
        currentState => {
          put(no, d)
          updateState(d, currentState)(no)
        })
    }

    case r @ Credited(no, amount, _) => Task {
      validateCredit(no).fold[Account](
        err => throw new RuntimeException(err),
        currentState => {
          put(no, r)
          updateState(r, currentState)(no)
        })
    }

  }
}