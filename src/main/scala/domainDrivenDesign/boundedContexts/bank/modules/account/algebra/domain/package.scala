package domainDrivenDesign.boundedContexts.bank.modules.account.algebra

import domainDrivenDesign.Abstractions.{Aggregate, AggregateCompanion, Event}

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

    object Account extends AggregateCompanion[Account] {
      override def empty: Account = Account("id", "name", DateTime.now())
    }
  }

  object events {
    import model.Account
    case class Closed(id: String, closeDate: Option[DateTime], at: DateTime = DateTime.now()) extends Event[Account]
    case class Credited(id: String, amount: BigDecimal, at: DateTime = DateTime.now()) extends Event[Account]
    case class Opened(id: String, name: String, openingDate: Option[DateTime], at: DateTime = DateTime.now()) extends Event[Account]
    case class Debited(id: String, amount: BigDecimal, at: DateTime = DateTime.now()) extends Event[Account]
  }

}
