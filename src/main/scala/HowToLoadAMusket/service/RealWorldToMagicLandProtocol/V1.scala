package HowToLoadAMusket.service.Protocol

import HowToLoadAMusket.service.MagicTypedLand.ActorPersistance.MusketEntity.Commands._

object V1 extends Protocol {
  val translation: Map[String, Command] = Map(
    "Prepare!" -> Prepare,
    "Take aim!" -> Aim,
    "Fire!" -> Shoot
  )
}
