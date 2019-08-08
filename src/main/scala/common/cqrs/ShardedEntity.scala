package common.cqrs

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import poc.model.ddd.{Command, Query}

import scala.math.abs

trait ShardedEntity {

  val props: Props

  val typeName: String

  // Factory Method for AggregateObjeto
  def start(implicit system: ActorSystem) = ClusterSharding(system).start(
    typeName        = this.typeName,
    entityProps     = this.props,
    settings        = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId  = extractShardId(1)
  )

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case qry: Query => (qry.aggregateRoot, qry)
    case cmd: Command => (cmd.aggregateRoot, cmd)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case qry: Query => (qry.aggregateRoot.toLong % numberOfShards).toString
    case cmd: Command => (cmd.aggregateRoot.toLong % numberOfShards).toString
  }
}

