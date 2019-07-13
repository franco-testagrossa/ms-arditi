package HowToLoadAMusket.api

trait MusketState
case object Unprepared extends MusketState
case object Prepared extends MusketState

trait Musket {
  def prepare(musket: Unprepared): Prepared
}
