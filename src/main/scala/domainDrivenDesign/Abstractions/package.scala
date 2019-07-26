package domainDrivenDesign

import scalaz.Free

package object Abstractions {

  type Command[A] = Free[Event, A]
}
