package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain

import domainDrivenDesign.Abstractions.{CommandF, Commands}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.commands._
import org.joda.time.DateTime

trait AccountCommands extends Commands[Account] {

  def open(no: String, name: String, openingDate: Option[DateTime]): CommandF[Account] =
    Open(no, name, openingDate, DateTime.now())

  def close(no: String, closeDate: Option[DateTime]): CommandF[Account] =
    Close(no, closeDate, DateTime.now())

  def debit(no: String, amount: BigDecimal): CommandF[Account] =
    Debit(no, amount, DateTime.now())

  def credit(no: String, amount: BigDecimal): CommandF[Account] =
    Credit(no, amount, DateTime.now())
}
