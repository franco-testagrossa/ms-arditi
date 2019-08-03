package sagas.utils

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


/**
  * Presents the interface for all specs that requires an ActorSystem
  * @param _system the unique system for the whole test suite (ClusterArditiSystem.system)
  */
abstract class ClusterArditiSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll
    with ImplicitSender {

  def this() = this(ClusterArditiSystem.system)
}
