package HowToLoadAMusket.service.RealWorldToMagicLandProtocol

import HowToLoadAMusket.service.MagicTypedLand.ActorPersistance.MusketEntity.Commands.Command

trait Protocol {
  val translation: Map[String, Command]
}
