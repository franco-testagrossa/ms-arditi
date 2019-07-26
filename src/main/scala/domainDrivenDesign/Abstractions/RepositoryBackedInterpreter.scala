package domainDrivenDesign.Abstractions

import scalaz.concurrent.Task
import scalaz.{ Free, \/, ~> }

trait RepositoryBackedInterpreter {
  def step: Event ~> Task

  def apply[A](action: Command[A]): Task[A] = action.foldMap(step)
}
