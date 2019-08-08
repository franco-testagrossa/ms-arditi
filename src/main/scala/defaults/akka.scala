package defaults

import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }

object akkaDefaults {

  def actorSystem(
      name:    String = "ClusterArditi",
      port:    Int    = 2552,
      specify: Config = ConfigFactory.parseString("")
  ): ActorSystem = {
    val config: Config = ConfigFactory
      .parseString(s"""akka.cluster.seed-nodes.0="akka.tcp://$name@127.0.0.1:$port"""")
      .withFallback(specify)
      .withFallback(ConfigFactory.load("application.conf"))
    ActorSystem(name, config)
  }
}
