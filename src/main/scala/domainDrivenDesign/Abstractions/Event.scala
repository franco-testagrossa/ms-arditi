package domainDrivenDesign.Abstractions

trait Event[A] {

  import org.joda.time.DateTime
  def at: DateTime
}