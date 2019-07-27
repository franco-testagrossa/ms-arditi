package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.algebra

import domainDrivenDesign.Abstractions.{Command, CommandHandler, Event, EventStore}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.events.{Credited, Debited, Opened}
import org.joda.time.DateTime
import scalaz.{Free, Scalaz, \/, ~>}
import scalaz.concurrent.Task
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._
import domainDrivenDesign.boundedContexts.bank.modules.account.interpreter.snapshot.AccountSnapshot.{snapshot, updateState}

trait AccountRules extends CommandHandler {

  def closed(a: Account): Error \/ Account

  def beforeOpeningDate(a: Account, cd: Option[DateTime]): Error \/ Account

  def sufficientFundsToDebit(a: Account, amount: BigDecimal): Error \/ Account

  def validateClose(id: String, cd: Option[DateTime]): Error \/ Map[String, Account]

  def validateDebit(id: String, amount: BigDecimal): Error \/ Map[String, Account]

  def validateCredit(id: String): Error \/ Map[String, Account]

  def validateOpen(id: String): _root_.scalaz.\/[_ <: _root_.scala.Predef.String, _ <: _root_.scala.Predef.String]
}