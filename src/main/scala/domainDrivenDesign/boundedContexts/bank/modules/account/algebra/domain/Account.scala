package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain
import domainDrivenDesign.Abstractions._

import org.joda.time.DateTime

case class Account(
  id: String,
  name: String,
  dateOfOpening: DateTime,
  dateOfClosing: Option[DateTime] = None,
  balance: Balance = Balance()) extends Aggregate