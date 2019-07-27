package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter

import akka.persistence.PersistentActor
import domainDrivenDesign.Abstractions.{BussinessRule, BussinessRules, Cmd, Event, Response, State, SuccessResponse}
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model.Account
import org.joda.time.DateTime
import scalaz.Scalaz._
import scalaz.\/

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
      state.verify(cmd).flatMap { response =>
        // persist(response.events.head) { evt => }.right
        val evt = response.events.head
        println("Persisted")
        state += evt
        println(s"State updated with $state and $evt")
        sender() ! response // add state to response
        println("sent to sender")
        response.right
      }
  }

  override def receiveRecover: Receive = {
    case evt: Event[A] => state += evt
  }
}

class MyEntity extends PersistentEntity[Account] {
  override var state: State[Account] =
    MyEntity.MyState(Account(persistenceId, "MyEntity", DateTime.now))
}
object MyEntity {
  // Cmds
  case class Run(id: String) extends Cmd[Account]
  // Events
  case class Runned(id: String, at: DateTime = DateTime.now) extends Event[Account]
  // Responses
  case class MyResponse(success: SuccessResponse, events: List[Event[Account]] = Nil) extends Response[Account]
  // BussinessRules
  object BussinessRuleA extends BussinessRule[Account] {
    // what happens with the list ?? how do i add more elements to it ?
    override def rule: PartialFunction[(Cmd[Account], State[Account]), Response[Account]] = {
      case (cmd: Run, state: State[Account]) =>
        MyResponse("Exito!!", List(Runned(cmd.id)))
    }
  }
  // State
  case class MyState(aggregate: Account) extends State[Account] with BussinessRules[Account] {
    override def +(event: Event[Account]): State[Account] = event match {
      case Runned(id, date) =>
        println(s"Runned $id ${date.toString}")
        this.copy(aggregate.copy(name = s"PARA VOS GIL $id"))
    }
    override val rules: List[BussinessRule[Account]] = List(BussinessRuleA) // add twice what happens ?
  }
}