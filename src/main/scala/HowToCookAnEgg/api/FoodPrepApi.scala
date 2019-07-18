package eggs.api

import eggs.domain.Egg.CookedEgg
import eggs.domain.{EggStyle}

import scala.concurrent.{Future}

trait FoodPrepApi{
  def prepareEgg(style: EggStyle): Future[CookedEgg] = ???
}
