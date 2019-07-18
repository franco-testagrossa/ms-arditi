package common.cassandra.utils

import java.io.File
import java.util.concurrent.CountDownLatch

import akka.persistence.cassandra.testkit.CassandraLauncher

import scala.concurrent.{ Future }

object Startup {

  def start(): Future[Unit] = {
    val databaseDirectory = new File("target/cassandra-db")
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = true,
      port  = 9042
    )
    println("Started Cassandra, press Ctrl + C to kill")
    new CountDownLatch(1).await()
    Future.successful(())

  }

}
