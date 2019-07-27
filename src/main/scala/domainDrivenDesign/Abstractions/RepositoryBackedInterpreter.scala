package domainDrivenDesign.Abstractions

import scalaz.concurrent.Task
import scalaz.{ Free, \/, ~> }

trait RepositoryBackedInterpreter {
  def step: Command ~> Task

  def apply[A](action: CommandF[A]): Task[A] = action.foldMap(step)
}
