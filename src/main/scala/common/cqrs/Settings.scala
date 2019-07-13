package cqrs

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.typesafe.sslconfig.util.EnrichedConfig

import scala.concurrent.duration.FiniteDuration

object Settings extends ExtensionId[Settings] with ExtensionIdProvider {
  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) = new Settings(system)
}

class Settings(system: ExtendedActorSystem) extends Extension {

  object eventProcessorSettings {
    private val eventProcessorConfig: EnrichedConfig =
      EnrichedConfig(system.settings.config.getConfig("event-processor"))

    val id: String = eventProcessorConfig.get[String]("id")

    val keepAliveInterval: FiniteDuration = eventProcessorConfig.get[FiniteDuration]("keep-alive-interval")

    val tagPrefix: String = eventProcessorConfig.get[String]("tag-prefix")

    val parallelism: Int = eventProcessorConfig.get[Int]("parallelism")
  }

  object entitySettings {
    private val entityConfig: EnrichedConfig =
      EnrichedConfig(system.settings.config.getConfig("entity"))

    val id: String = entityConfig.get[String]("id")
    val shardCount: Int = entityConfig.get[Int]("shard-count")
  }

}
