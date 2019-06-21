package eggs.domain


sealed trait Egg

object Egg {
  case object RawEgg extends Egg

  sealed trait CookedEgg extends Egg

  case class PartiallyCookedEgg (style: EggStyle) extends CookedEgg
  case class FullyCookedEgg private[Egg](style: EggStyle) extends CookedEgg
}
