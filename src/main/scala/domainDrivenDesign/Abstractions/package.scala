package domainDrivenDesign

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

  trait State[S] {
    def +[A](event: Event[A]): State[S]
    def get: State[S]
    def verify[A](command: Commands[A]): Option[State[S]]
  }

  trait PersistentEffect[A] {

  }

}