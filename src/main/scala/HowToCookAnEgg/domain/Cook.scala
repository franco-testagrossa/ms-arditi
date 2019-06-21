package eggs.domain

import Egg.CookedEgg

import scala.concurrent.{Future}

case class Cook(name: String = "Cosmofulanito")

object Cook {

  def prepareEgg(style: EggStyle): Future[CookedEgg] = ???
}
