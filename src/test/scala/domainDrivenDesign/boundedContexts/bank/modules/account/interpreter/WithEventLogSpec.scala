package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain._
import scalaz._
import common.io.persistence.inMemoryEventStore
import scalaz.concurrent.Task
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._

class WithEventLogSpec extends org.scalatest.WordSpec {

  object withEventLog extends App with AccountRulesWithMockDB with inMemoryEventStore {
    val result: Task[Account] = apply(comp)
    val complete: Account = result.unsafePerformSync
    val events = allEvents

    events.map(
      events => println(
        events.reverse.mkString("\n")))
  }

  "WithEventLog" should {
    "do something" in {
      withEventLog.main(Array())
    }
  }
}