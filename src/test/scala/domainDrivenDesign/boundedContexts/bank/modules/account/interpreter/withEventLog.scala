package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import domainDrivenDesign.Abstractions.{Command, Event}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain._
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.command.AccountCommands
import scalaz._
import Scalaz._
import common.io.persistence.inMemoryEventStore
import org.joda.time.DateTime
import scalaz.concurrent.Task

object withEventLog extends App with AccountCommands with inMemoryEventStore {

  def transfer(from: String, to: String, amount: BigDecimal): Command[Unit] = for {
    _ <- debit(from, amount)
    _ <- credit(to, amount)
  } yield ()

  val composite =
    for {
      a <- open("a-123", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 23000)
    } yield d

  val compositeFail =
    for {
      a <- open("a-124", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 50000)
    } yield d

  val comp: Free[Event, Account] =
    for {
      a <- open("a1", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 23000)
    } yield d

  val result: Task[Account] = AccountRulesWithMockDB(comp)
  val complete: Account = result.unsafePerformSync
  val events = AccountRulesWithMockDB.allEvents

  events.map(
    events => println(
      events.reverse.mkString("\n")))

}
