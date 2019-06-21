
package eggs.domain

sealed trait Egg

object Egg {
  case object RawEgg extends Egg {
    def startCooking(style: EggStyle): PartiallyCookedEgg = PartiallyCookedEgg(style)
  }

  sealed trait CookedEgg extends Egg {
    def style: EggStyle
  }

  case class PartiallyCookedEgg(style: EggStyle) extends CookedEgg
  case class FullyCookedEgg(style: EggStyle) extends CookedEgg
}
