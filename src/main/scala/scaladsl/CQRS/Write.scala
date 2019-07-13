package scaladsl.CQRS

import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import akka.persistence.cassandra.testkit.CassandraLauncher
import com.typesafe.config.{ Config, ConfigFactory }
import cqrs.{ CassandraSessionExtension, EventProcessorWrapper, ShardedEntity }
import scaladsl.CQRS.example.ExampleEntity

import scala.collection.breakOut
import scala.concurrent.Await
import scala.concurrent.duration._

object App extends ShardedEntity {

  override val entityProps: Props = ExampleEntity.props
  private val Opt = """(\S+)=(\S+)""".r

  private def argsToOpts(args: Seq[String]): Map[String, String] =
    args.collect { case Opt(key, value) => key -> value }(breakOut)

  private def applySystemProperties(options: Map[String, String]): Unit =
    for ((key, value) <- options if key startsWith "-D")
      System.setProperty(key.substring(2), value)

  def main(args: Array[String]): Unit = {

    val opts = argsToOpts(args.toList)
    applySystemProperties(opts)

    args.headOption match {

      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt

        startNode(port)

      case Some("cassandra") =>
        startCassandraDatabase()
        println("Started Cassandra, press Ctrl + C to kill")
        new CountDownLatch(1).await()

    }
  }

  def startNode(port: Int): Unit = {

    val system = ActorSystem("ClusterArditi", config(port))

    val selfRoles = Cluster(system).selfRoles

    if (selfRoles.contains("write-model")) {
      ShardedEntity(system).start()
      testIt(system)
    }

    if (selfRoles.contains("read-model")) {
      createTables(system)
      EventProcessorWrapper(system).start()
    }
  }

  def config(port: Int): Config =
    ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.port = $port
          akka.remote.netty.tcp.port = $port
       """
    ).withFallback(ConfigFactory.load("application.conf"))

  def startCassandraDatabase(): Unit = {
    val databaseDirectory = new File("target/cassandra-db")
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = true,
      port  = 9042
    )
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

  // FIXME very temporary test
  def testIt(system: ActorSystem): Unit = {

    val shardedEntity = ShardedEntity(system)

    shardedEntity ! ("switch", ExampleEntity.CreateSwitch(4))
    shardedEntity ! ("switch1", ExampleEntity.SetPortStatus(2, portEnabled = true))
    shardedEntity ! ("switch1", ExampleEntity.SendPortStatus)

    shardedEntity ! ("switch2", ExampleEntity.CreateSwitch(6))
    shardedEntity ! ("switch2", ExampleEntity.SetPortStatus(0, portEnabled = true))
    shardedEntity ! ("switch2", ExampleEntity.SetPortStatus(2, portEnabled = true))
    shardedEntity ! ("switch2", ExampleEntity.SetPortStatus(5, portEnabled = true))
    shardedEntity ! ("switch2", ExampleEntity.SendPortStatus)

    val counter = new AtomicInteger(8)
    import system.dispatcher
    system.scheduler.schedule(3 seconds, 1 second) {
      import scala.util.Random
      val switch = s"switch${counter.getAndIncrement()}"
      shardedEntity ! (switch, ExampleEntity.CreateSwitch(6))
      shardedEntity ! (switch, ExampleEntity.SetPortStatus(Random.nextInt(6), portEnabled = true))
      shardedEntity ! (switch, ExampleEntity.SetPortStatus(Random.nextInt(6), portEnabled = true))
      shardedEntity ! (switch, ExampleEntity.SendPortStatus)
    }
  }
}
