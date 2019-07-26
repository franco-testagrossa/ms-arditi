package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter.snapshot

import domainDrivenDesign.Abstractions.{ Event, Snapshot }
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.{ Account, Balance }
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.event.{ Closed, Credited, Debited, Opened }

object AccountSnapshot extends Snapshot[Account] {
  override def updateState(e: Event[_], initial: Map[String, Account]): Map[String, Account] = e match {

    case o @ Opened(no, name, odate, _) =>
      initial + (no -> Account(no, name, odate.get))

    case c @ Closed(no, cdate, _) =>
      initial + (no -> initial(no).copy(dateOfClosing = cdate))

    case d @ Debited(no, amount, _) =>
      val a = initial(no)
      initial + (no -> a.copy(balance =
        Balance(a.balance.amount - amount)))

    case r @ Credited(no, amount, _) =>
      val a = initial(no)
      initial + (no -> a.copy(balance =
        Balance(a.balance.amount + amount)))
  }
}
