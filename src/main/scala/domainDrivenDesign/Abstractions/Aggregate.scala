package domainDrivenDesign.Abstractions

/**
 * All aggregates need to have an id
 */
trait Aggregate {
  type AggregateRoot = String
  def id: AggregateRoot
}
