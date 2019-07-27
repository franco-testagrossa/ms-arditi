package domainDrivenDesign.Abstractions

import scalaz.concurrent.Task
import scalaz.~>

trait CommandHandler {
  val step: Event ~> Task = new (Event ~> Task) {
    override def apply[A](action: Event[A]): Task[A] = handleCommand(action)
  }

  def apply[A](action: Command[A]): Task[A] = action.foldMap(step)

  // extrae el evento del command
  def handleCommand[A](e: Event[A]): Task[A]
}
