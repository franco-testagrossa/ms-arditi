package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import domainDrivenDesign.Abstractions.{CommandF, Event}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.AccountCommands
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter.AccountRulesV1
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._
import org.joda.time.DateTime
import scalaz._

trait AccountRulesWithMockDB extends AccountCommands with AccountRulesV1 {

  def transfer(from: String, to: String, amount: BigDecimal): CommandF[Unit] = for {
    _ <- debit(from, amount)
    _ <- credit(to, amount)
  } yield ()

  def composite =
    for {
      a <- open("a-123", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 23000)
    } yield d

  def compositeFail =
    for {
      a <- open("a-124", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 50000)
    } yield d

  def comp: CommandF[Account] =
    for {
      a <- open("a1", "debasish ghosh", Some(DateTime.now()))
      _ <- credit(a.id, 10000)
      _ <- credit(a.id, 30000)
      d <- debit(a.id, 23000)
    } yield d
}