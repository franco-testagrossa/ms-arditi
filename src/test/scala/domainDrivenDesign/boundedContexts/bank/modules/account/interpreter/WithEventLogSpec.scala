package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import akka.persistence.PersistentActor
import domainDrivenDesign.Abstractions._
import scalaz.concurrent.Task
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter.AccountRulesV1
import scalaz.Scalaz._
import scalaz.\/

class WithEventLogSpec extends org.scalatest.WordSpec {

  object withEventLog extends App {
    import AccountRulesWithMockDB._
    import AccountRulesV1._

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


  // DSL IN ACTION
  // Persistent entity for Aggregate Account
  trait PersistentEntity[A] extends PersistentActor {
    val typeName = self.getClass.getSimpleName
    override def persistenceId: String = typeName  + "-" + self.path.name

    // interpreter: Interpreter[A]
    var state: State[A]
    override def receiveCommand: Receive = {
      case cmd: Cmd[A] =>
        // val output: P[E] = interpreter.run(cmd, state)
        val response = state.verify(cmd)
        persist(response.events.head) { evt =>
          state += evt
          println(state)
          sender() ! state
        }
    }

    override def receiveRecover: Receive = {
      case evt: Event[A] => state += evt
    }
  }
}



