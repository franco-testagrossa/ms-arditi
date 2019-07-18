package scaladsl.KafkaWithAkka.exampleBoundedContext.HowToCookAnEgg.runner

import akka.actor.Actor
import eggs.domain.EggStyle
import eggs.domain.EggStyle._

trait Adaptor {

  def toEggStyle(
      message: Any,
      default: EggStyle = HardBoiled
  ): EggStyle = message match {
    case "Scrambled" => Scrambled
    case "OverEasy" => OverEasy
    case "OverMedium" => OverMedium
    case "OverHard" => OverHard
    case "HardBoiled" => HardBoiled
    case "SoftBoiled" => SoftBoiled
    case "Poached" => Poached
    case _ => default
  }
}

