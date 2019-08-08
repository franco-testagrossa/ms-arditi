package domainDrivenDesign.Abstractions

import scalaz.\/

trait EventStore[Key] {
  /** gets the list of events for an aggregate key `key`
    */
  def get(key: Key): List[Event[_]]

  /** puts a `key` and its associated `event`
    */
  def put(key: Key, event: Event[_]): Error \/ List[Event[_]]

  /** similar to `get` but returns an error if the `key` is not found
    */
  def events(key: Key): Error \/ List[Event[_]]

  /** get all ids from the event store
    */
  def allEvents: Error \/ List[Event[_]]
}
