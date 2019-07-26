package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.command

import domainDrivenDesign.Abstractions.{Command, Commands}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.Account
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.event.{Closed, Credited, Debited, Opened}
import org.joda.time.DateTime

trait AccountCommands extends Commands[Account] {

  def open(no: String, name: String, openingDate: Option[DateTime]): Command[Account] =
    Opened(no, name, openingDate, DateTime.now())

  def close(no: String, closeDate: Option[DateTime]): Command[Account] =
    Closed(no, closeDate, DateTime.now())

  def debit(no: String, amount: BigDecimal): Command[Account] =
    Debited(no, amount, DateTime.now())

  def credit(no: String, amount: BigDecimal): Command[Account] =
    Credited(no, amount, DateTime.now())
}
