package HowToLoadAMusket.api

import HowToLoadAMusket.domain.MusketBall

import scala.concurrent.Future
import HowToLoadAMusket.domain.MusketState._
import HowToLoadAMusket.repository.MusketBallRepository.{ fetch => fetchMusketball }

import scala.concurrent.ExecutionContext.Implicits.global

trait MusketApi {

  def volley(musket: Unprepared): Future[Shot] = for {
    prepared <- prepare(musket)
    aimed <- aim(prepared)
    shot <- shoot(aimed)
  } yield (shot)

  def prepare(musket: Unprepared): Future[Prepared] = for {
    musketBall <- fetchMusketball()
    preparedMusket <- load(musketBall)
  } yield (preparedMusket)

  def load(musketBall: MusketBall): Future[Prepared] = Future {
    Prepared()
  }

  def aim(musket: Prepared): Future[Aimed] = Future {
    Aimed()
  }

  def shoot(musket: Aimed): Future[Shot] = Future {
    Shot()
  }
}
