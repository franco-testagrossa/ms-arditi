package domainDrivenDesign

import scalaz.{-\/, \/, \/-}
import scalaz.Scalaz._


package object Abstractions {
  /**
    * All aggregates need to have an id
    */
  type AggregateRoot = String
  trait Aggregate {
    def id: AggregateRoot
  }
  trait AggregateCompanion[A <: Aggregate] { // ???
    def empty: A
  }

  // Commands Algebra
  type Command[A] = scalaz.Free[Event, A]
  trait Commands[A] {
    implicit def liftCommand(event: Event[A]): Command[A] =
      scalaz.Free.liftF[Event, A](event)
  }

  // DSL => missing [A <: Aggregate] due to Commands
  trait Event[A] {
    import org.joda.time.DateTime
    // def id: AggregateRoot ???
    def at: DateTime
  }

  trait Cmd[A] {
    def id: AggregateRoot
  }

  case class Response[A](success: String, event: Event[A])

  type BsResponse[A] = \/[String, Response[A]]
  type BusinessRule[A] = (Cmd[A], State[A]) => BsResponse[A]
  trait BusinessRules[A] extends ((Cmd[A], State[A]) => BsResponse[A]) {
    val rules: List[BusinessRule[A]]
    def apply(cmd: Cmd[A], state: State[A]): BsResponse[A] = {
      // traverse and rull all rules until we get an error
      def traverse(bag: BsResponse[A], st: State[A],
                   rulez: List[BusinessRule[A]]): BsResponse[A] =
        rulez match {
          case Nil => bag
          case rule :: rest => rule(cmd, st) match {
            case -\/(error) => error.left
            case \/-(response@Response(_, ev)) =>
              traverse(response.right, st + ev, rest)
          }
        }
      traverse("Rules is Empty".left, state, rules)
    }
  }

  trait State[A] { bs: BusinessRules[A] =>
    // State Managment
    val aggregate: A
    def +(event: Event[A]): State[A]
    // BussinessRules
    def verify(command: Cmd[A]): BsResponse[A] = bs(command, this)
  }
}