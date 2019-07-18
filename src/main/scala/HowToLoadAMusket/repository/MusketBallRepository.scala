package HowToLoadAMusket.repository

import HowToLoadAMusket.domain.MusketBall
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
object MusketBallRepository {
  def fetch(): Future[MusketBall] = Future {
    MusketBall()
  }
}
