package sagas.utils

import akka.actor.ActorSystem


/**
  * Represents the ActorSystem for the whole test-suite to avoid port collision when running tests in parallel.
  * Tests which requires an ActorSystem should extend from ClusterArditiSpec
  */
final object ClusterArditiSystem {
  final lazy val system = ActorSystem("ClusterArditi")
}
