package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import akka.persistence.PersistentActor
import common.io.persistence.inMemoryEventStore
import domainDrivenDesign.Abstractions._
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


  // DSL IN ACTION
  // Persistent entity for Aggregate Account
  trait PersistentEntity[A] extends PersistentActor {
    val typeName = self.getClass.getSimpleName
    override def persistenceId: String = typeName  + "-" + self.path.name

    var state: State[A]
    override def receiveCommand: Receive = {
      case cmd: Commands[_] =>

    }

    override def receiveRecover: Receive = {
      case evt: Event[_] => state += evt
    }
  }

  object CompanionPersistentEntity {

    // object PersistentEntityState extends State[Account] {
    //   override def +[A](event: Event[A]): State[Account] = ???
    // }
  }
}



