package utils

import akka.actor.ActorSystem

object ClusterArditiSystem {
  lazy val system = ActorSystem("ClusterArditi")
}
