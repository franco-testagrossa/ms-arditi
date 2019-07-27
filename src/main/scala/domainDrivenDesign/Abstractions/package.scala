package domainDrivenDesign

import domainDrivenDesign.Abstractions.{Cmd, Response, State}
import scalaz.{Validation, \/}


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
    def +(event: Event[A]): State[A]
    def get: State[A]
    def aggregate: A
    def verify(command: Cmd[A]): Response[A] = bs(command, this)
  }

  trait Response[A] {
    def errors: List[String]
    def events: List[Event[A]]
    def success: String
    def error: String
  }

  trait BussinessRule[A] {
    def rule: PartialFunction[(Cmd[A], State[A]), Response[A]]
  }

  trait BussinessRules[A] {
    val rules: List[BussinessRule[A]]
    def apply(cmd: Cmd[A], state: State[A]): Response[A] = {
      val value: PartialFunction[(Cmd[A], State[A]), Response[A]] =
        rules.reduce((a, b) => a.rule.orElse(b.rule))

      val response: Response[A] = value.apply(cmd,state)
      response
    }

  }
}