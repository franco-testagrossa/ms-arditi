package domainDrivenDesign.Abstractions

import scalaz.concurrent.Task
import scalaz.~>

trait CommandHandler {
  def step: Event ~> Task

  def apply[A](action: Command[A]): Task[A] = action.foldMap(step)

  // extrae el evento del command
  def handleCommand[A](e: Event[A]): Task[A]
}
