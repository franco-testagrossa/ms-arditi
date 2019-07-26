package domainDrivenDesign.Abstractions

import scalaz._
import Scalaz._

trait Snapshot[A <: Aggregate] {
  def updateState(e: Event[_], initial: Map[String, A]): Map[String, A]

  def snapshot(es: List[Event[_]]): Error \/ Map[String, A] =
    es.reverse.foldLeft(Map.empty[String, A]) { (a, e) => updateState(e, a) }.right
}

