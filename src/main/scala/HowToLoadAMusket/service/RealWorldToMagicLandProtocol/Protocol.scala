package HowToLoadAMusket.service.Protocol

import HowToLoadAMusket.service.MagicTypedLand.ActorPersistance.MusketEntity.Commands.Command

trait Protocol {
  val translation: Map[String, Command]
}
