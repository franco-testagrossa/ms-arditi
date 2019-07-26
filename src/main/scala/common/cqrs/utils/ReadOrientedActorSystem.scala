package common.cqrs.utils

import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }

trait ReadOrientedActorSystem extends ActorSystemFactory {
  val port = 2551
  override val role = "read"
  override val lead = false
  override val index = 1

  override def createActorSystem(
    name: String = "ClusterArditi",
    config: Config = ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port = $port
      akka.remote.netty.tcp.port = $port
      akka.cluster.roles.0=${role}-model
      akka.cluster.roles.1=${if (lead) "static" else "dynamic"}
      akka.persistence.journal.plugin=cassandra-journal
      akka.persistence.snapshot-store.plugin=cassandra-snapshot-store
      application.api.host=127.0.0.$index
      application.api.port=8080
      akka.cluster.seed-nodes.0="akka://ClusterArditi@127.0.0.1:2551"
      akka.discovery.method=config
      akka.management.http.hostname=127.0.0.$index
      akka.remote.artery.canonical.hostname=127.0.0.$index
   """).withFallback(ConfigFactory.load("application.conf"))): ActorSystem = ActorSystem(name, config)
}
