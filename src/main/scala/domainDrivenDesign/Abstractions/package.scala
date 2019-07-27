package domainDrivenDesign

import domainDrivenDesign.Abstractions.{Cmd, Response, State}
import scalaz.{Validation, \/}
import scalaz.Scalaz._


package object Abstractions {
  /**
    * All aggregates need to have an id
    */
  type AggregateRoot = String
  trait Aggregate {
    def id: AggregateRoot
  }
  trait AggregateCompanion[A <: Aggregate ] {
    def empty: A
  }

  trait Event[A] {
    import org.joda.time.DateTime
    def at: DateTime
  }

  // Commands Algebra
  type Command[A] = scalaz.Free[Event, A]
  trait Commands[A] {
    implicit def liftCommand(event: Event[A]): Command[A] =
      scalaz.Free.liftF[Event, A](event)
  }

  // DSL
  trait Cmd[A] {
    def id: AggregateRoot
  }

  trait State[A] { bs: BussinessRules[A] =>
    def aggregate: A
    def +(event: Event[A]): State[A]
    def verify(command: Cmd[A]): String \/ Response[A] = bs(command, this)
  }

  type SuccessResponse = String
  type ErrorResponse = String
  case class Response[A](success: SuccessResponse, events: List[Event[A]])

  trait BussinessRule[A] {
    def rule: PartialFunction[(Cmd[A], State[A]), String \/ Response[A]]
  }

  trait BussinessRules[A] {
    val rules: List[BussinessRule[A]]
    def apply(cmd: Cmd[A], state: State[A]): String \/ Response[A] =
        rules
          .map(_.rule)
          .reduce(_ orElse _)
          .apply((cmd, state))
  }
}