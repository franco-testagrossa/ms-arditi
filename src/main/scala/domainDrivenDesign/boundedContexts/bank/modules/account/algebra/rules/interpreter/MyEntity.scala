package domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import domainDrivenDesign.Abstractions._
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model.Account
import org.joda.time.DateTime
import scalaz.Scalaz._
import scalaz.\/

// DSL IN ACTION
// Persistent entity for Aggregate Account
trait PersistentEntity[A] extends PersistentActor with ActorLogging {
  val typeName = self.getClass.getSimpleName
  override def persistenceId: String = typeName  + "-" + self.path.name

  // interpreter: Interpreter[A]
  var state: State[A] // Option and None as default ?
  override def receiveCommand: Receive = {
    case cmd: Cmd[A] =>
      log.info("Received command {}", cmd)
      // val output: P[E] = interpreter.run(cmd, state)
      state.verify(cmd).flatMap { response =>
        persist(response.event) { e =>
          log.info("Persisted")
          state += e
          log.info("State updated with {} and {}", state, e)
          sender() ! response // add state to response
          println("sent to sender")
        }
        ().right
      }
  }

  override def receiveRecover: Receive = {
    case evt: Event[A] => state += evt
  }
}

class MyEntity extends PersistentEntity[Account] {
  override def persistenceId: String = "MyEntity"  + "-" + self.path.name

  override var state: State[Account] =
    MyEntity.MyState(Account(persistenceId, "MyEntity", DateTime.now))
}


object MyEntity {
  // Cmds
  case class Run(id: String) extends Cmd[Account]
  // Events
  case class Runned(id: String, at: DateTime = DateTime.now) extends Event[Account]
  // BussinessRules
  object BusinessRuleA extends ((Cmd[Account], State[Account]) => BsResponse[Account]) {
    override def apply(cmd: Cmd[Account], state: State[Account]): BsResponse[Account] =
      Response("Exito!!", Runned(cmd.id)).right
  }
  // State
  case class MyState(aggregate: Account) extends State[Account] with BusinessRules[Account] {
    override def +(event: Event[Account]): State[Account] = event match {
      case Runned(id, date) =>
        println(s"Runned $id ${date.toString}")
        this.copy(aggregate.copy(name = s"PARA VOS GIL $id")) // Lens
    }
    override val rules: List[BusinessRule[Account]] = List(BusinessRuleA) // add twice what happens ?
  }

  // Sharding
  def props(): Props = Props(new MyEntity)
}