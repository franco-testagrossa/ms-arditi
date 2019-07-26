package domainDrivenDesign.Abstractions

import scalaz.Free

trait Commands[A] {
  implicit def liftEvent[A](event: Event[A]): Command[A] = Free.liftF(event)
}


