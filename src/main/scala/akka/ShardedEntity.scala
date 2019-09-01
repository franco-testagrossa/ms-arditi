package akka

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import model.ddd._

trait ShardedEntity {

  val typeName: String
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case qry: Query => (qry.aggregateRoot, qry)
    case cmd: Command => (cmd.aggregateRoot, cmd)
  }

  def props(): Props

  // Factory Method for Product
  def start(implicit system: ActorSystem): ActorRef = ClusterSharding(system).start(
    typeName        = typeName,
    entityProps     = props(),
    settings        = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId  = extractShardId(1)
  )

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case qry: Query => (qry.aggregateRoot.toDouble % numberOfShards).toString
    case cmd: Command => (cmd.aggregateRoot.toDouble % numberOfShards).toString
  }
}

