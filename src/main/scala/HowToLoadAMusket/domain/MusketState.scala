package HowToLoadAMusket.domain

sealed trait MusketState

object MusketState {
  case class Unprepared() extends MusketState
  case class Prepared() extends MusketState
  case class Aimed() extends MusketState
  case class Shot() extends MusketState
}
