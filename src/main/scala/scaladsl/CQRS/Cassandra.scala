package scaladsl.CQRS

import akka.actor.{ ActorSystem }
import akka.cluster.Cluster
import com.typesafe.config.{ Config, ConfigFactory }
import cqrs.{ CassandraSessionExtension, EventProcessorWrapper, ShardedEntity }
import scala.concurrent.Await
import scala.concurrent.duration._

object Read extends App with ShardedEntity {
  private val port = 2551
  private val role = "read"
  private val lead = true
  private val index = 1
  val config: Config =
    ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port = $port
          akka.remote.netty.tcp.port = $port
          akka.cluster.roles.0=$role-model
          akka.cluster.roles.1=${if (lead) "static" else "dynamic"}
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

  val system = ActorSystem("ClusterArditi", config)

  val selfRoles = Cluster(system).selfRoles
  println("HERE        " + selfRoles)
  if (selfRoles.contains("read-model")) {
    createTables(system)
    EventProcessorWrapper(system).start()
  }

  def createTables(system: ActorSystem): Unit = {
    val session = CassandraSessionExtension(system).session

    // TODO use real replication strategy in real application
    val keyspaceStmt = """
      CREATE KEYSPACE IF NOT EXISTS akka_cqrs_sample
      WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
      """

    val offsetTableStmt =
      """
      CREATE TABLE IF NOT EXISTS akka_cqrs_sample.offsetStore (
        eventProcessorId text,
        tag text,
        timeUuidOffset timeuuid,
        PRIMARY KEY (eventProcessorId, tag)
      )
      """

    // ok to block here, main thread
    Await.ready(session.executeCreateTable(keyspaceStmt), 30 seconds)
    Await.ready(session.executeCreateTable(offsetTableStmt), 30 seconds)
  }

}
