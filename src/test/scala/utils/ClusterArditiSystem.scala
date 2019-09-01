package utils

import java.net.ServerSocket

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


/**
  * Represents the ActorSystem for the whole test-suite to avoid port collision when running tests in parallel.
  * Tests which requires an ActorSystem should extend from ClusterArditiSpec
  */
final object ClusterArditiSystem {
  final lazy val system = ActorSystem("ClusterArditi")


}
