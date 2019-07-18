package common.cassandra

import akka.actor.ActorSystem
import common.cqrs.CassandraSessionExtension
import common.cqrs.utils.ReadOrientedActorSystem

import scala.concurrent.{ Await }
import scala.concurrent.duration._

object CassandraDefaults extends ReadOrientedActorSystem {

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
