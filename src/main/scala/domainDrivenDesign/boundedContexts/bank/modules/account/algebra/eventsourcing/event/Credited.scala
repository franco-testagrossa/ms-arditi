package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.eventsourcing.event

import domainDrivenDesign.Abstractions.Event
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.Account
import org.joda.time.DateTime

case class Credited(no: String, amount: BigDecimal, at: DateTime = DateTime.now()) extends Event[Account]