package domainDrivenDesign.boundedContexts.bank.modules.account.algebra

import domainDrivenDesign.Abstractions.{Aggregate, Event}

package object domain {
  import org.joda.time.DateTime
  object model {

    case class Balance(amount: BigDecimal = 0)
    case class Account(
                        id: String,
                        name: String,
                        dateOfOpening: DateTime,
                        dateOfClosing: Option[DateTime] = None,
                        balance: Balance = Balance()) extends Aggregate
  }

  // this should exists inside the companion object for each persistent actor
  object events {
    import model.Account

    case class Closed(no: String, closeDate: Option[DateTime], at: DateTime = DateTime.now()) extends Event[Account]
    case class Credited(no: String, amount: BigDecimal, at: DateTime = DateTime.now()) extends Event[Account]
    case class Opened(no: String, name: String, openingDate: Option[DateTime], at: DateTime = DateTime.now()) extends Event[Account]
    case class Debited(no: String, amount: BigDecimal, at: DateTime = DateTime.now()) extends Event[Account]
  }
}
