package eggs.domain
import Egg._
import EggStyle.HardBoiled

trait DomainHelpers {
  def createRawEgg() = RawEgg

  def createPartiallyCookedEgg(style: EggStyle = createEggStyle()): PartiallyCookedEgg = PartiallyCookedEgg(style)

  def createCookedEgg(style: EggStyle = createEggStyle()): CookedEgg = FullyCookedEgg(style)
  def createEggStyle(): EggStyle = HardBoiled
}
