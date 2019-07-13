package eggs.api

import eggs.domain.Egg.{ CookedEgg, FullyCookedEgg }
import eggs.domain.EggStyle

import scala.concurrent.Future

trait FoodPrepApi {
  def prepareEgg(style: EggStyle): Future[CookedEgg] = Future.successful(FullyCookedEgg(style))
}
