package common.io.persistence

import domainDrivenDesign.Abstractions.{ Event, EventStore }
import scalaz.\/
import scalaz.Scalaz._

trait inMemoryEventStore extends EventStore[String] {

  import scala.collection.concurrent.TrieMap
  val eventLog = TrieMap[String, List[Event[_]]]()

  override def get(key: String): List[Event[_]] = eventLog.get(key).getOrElse(List.empty[Event[_]])

  override def put(key: String, event: Event[_]):  Error \/ List[Event[_]] = { // HELP - WHY DOES SCALAZ NOT WORK HERE
    val currentList = eventLog.getOrElse(key, Nil)
    eventLog += (key -> (event :: currentList))
    currentList.right
  }

  override def events(key: String): Error \/ List[Event[_]] = {
    val currentList = eventLog.getOrElse(key, Nil)
    if (currentList.isEmpty) new Error(s"Aggregate $key does not exist").left
    else currentList.right
  }
  def allEvents = eventLog.values.toList.flatten.right

}