package common.cqrs.utils

import akka.actor.ActorSystem
import com.typesafe.config.{ Config, ConfigFactory }

trait ActorSystemFactory {

  val role = "read"
  val lead = false
  val index = 1

  def createActorSystem(name: String, config: Config): ActorSystem
}

// honor composition over inheritance
object ActorSystemFactory {

  case class ActorSystemMembers(lead: ActorSystem)
  val actorSystems = Map.empty[String, ActorSystem]

  def createConfig(role: String, index: Int) = {
    val index = Seq(actorSystems).length + 1

    ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port = 2551
      akka.remote.netty.tcp.port = 2551
      akka.cluster.roles.0=${role}-model
      akka.cluster.roles.1=${if (Seq(actorSystems).length == 0) "static" else "dynamic"}
      akka.persistence.journal.plugin=cassandra-journal
      akka.persistence.snapshot-store.plugin=cassandra-snapshot-store
      application.api.host=127.0.0.$index
      application.api.port=8080
      akka.cluster.seed-nodes.0="akka://ClusterArditi@127.0.0.1:2551"
      akka.discovery.method=config
      akka.management.http.hostname=127.0.0.$index
      akka.remote.artery.canonical.hostname=127.0.0.$index
   """
    ).withFallback(ConfigFactory.load("application.conf"))

  }

  def write(name: String) = ???
}
