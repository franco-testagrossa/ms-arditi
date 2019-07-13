package cqrs.utils

import akka.actor.ActorSystem
import com.typesafe.config.{Config}

trait ActorSystemFactory {
  def createActorSystem(name: String, config: Config) : ActorSystem
}
