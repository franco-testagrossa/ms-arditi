package domainDrivenDesign

package object Abstractions {
  /**
    * All aggregates need to have an id
    */
  trait Aggregate {
    type AggregateRoot = String
    def id: AggregateRoot
  }
  trait AggregateCompanion[A <: Aggregate ] {
    def empty: A
  }

  trait Command[A] {
    def id: String
  }

  trait Event[A] {
    import org.joda.time.DateTime
    def at: DateTime
  }

  trait State[S <: Aggregate] {
    def +[A](event: Event[A]): State[S]
  }

  // Commands Algebra
  type CommandF[A] = scalaz.Free[Command, A]
  trait Commands[A] {
    implicit def liftCommand(command: Command[A]) =
      scalaz.Free.liftF[Command, A](command)
  }

  trait PersistentEffect[A] {

  }

}