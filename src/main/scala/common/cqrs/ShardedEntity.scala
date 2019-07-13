package cqrs

import akka.actor.{ ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import scaladsl.CQRS.example.ExampleEntity

import scala.math.abs

trait ShardedEntity {

  val entityProps: Props = ExampleEntity.props;

  object ShardedEntity extends ExtensionId[ShardedEntity] with ExtensionIdProvider {
    override def lookup: EventProcessorWrapper.type = EventProcessorWrapper

    override def createExtension(system: ExtendedActorSystem) = new ShardedEntity(system)

    case class EntityEnvelope(EntityId: String, payload: Any)

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case EntityEnvelope(id, msg) => (id, msg)
    }

    def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
      case EntityEnvelope(eventProcessorId, msg) => abs(eventProcessorId.hashCode % numberOfShards).toString
    }
  }

  class ShardedEntity(system: ExtendedActorSystem) extends Extension {

    import ShardedEntity._

    private val entitySettings = Settings(system).entitySettings

    private val typeName = entitySettings.id

    private val shardCount = entitySettings.shardCount

    def start(): Unit =
      ClusterSharding(system).start(
        typeName        = typeName,
        entityProps     = entityProps,
        settings        = ClusterShardingSettings(system).withRole("write-model"),
        extractEntityId = extractEntityId,
        extractShardId  = extractShardId(shardCount)
      )

    def tell(id: String, msg: Any)(implicit sender: ActorRef = ActorRef.noSender): Unit =
      shardRegion ! EntityEnvelope(id, msg)

    def !(id: String, msg: Any): Unit =
      tell(id, msg)

    def shardRegion: ActorRef = ClusterSharding(system).shardRegion(typeName)

  }

}
