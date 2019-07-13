package scaladsl.KafkaWithAkka.HowToCookAnEgg.runner

trait Waiter {

  def WaiterTalk(eggStyle: String) =
    Array(
      "", "I hope you enjoy the egg.", "Here in this place we serve the best eggs in the country. We are known for this.", "", "Here is the egg as you requested, sire.", s"... ${eggStyle}?"
    )
      .map(line => Console.YELLOW + line + Console.RESET)
      .mkString("\n")
      .stripMargin

  def withLisp(message: String) =
    message
      .replaceAll("s", "th") // The Actor suffers from lisp

  //https://hackernoon.com/operator-in-scala-cbca7b939fc0
  implicit class Pipe[A](val a: A) {
    def |>[B](f: A => B): B = f(a)
  }

  def DumbWaiterTalk(eggStyle: String) =
    WaiterTalk(eggStyle) |> withLisp
}

