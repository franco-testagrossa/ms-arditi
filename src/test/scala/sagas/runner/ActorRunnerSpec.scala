package sagas.runner

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sagas.utils.{ClusterArditiSpec, ClusterArditiSystem}

import scala.concurrent.duration._
import scala.language.postfixOps

class ActorRunnerSpec extends ClusterArditiSpec {

  "An ActorRunner" should {
    "return its path for every msg sent" in {
      val testProbe = TestProbe()
      val actorRunner = system.actorOf(Props[ActorRunner], "actor-sagas.runner")
      actorRunner.!("msg")(testProbe.ref)
      val expectedResponse: String = "akka://ClusterArditi/user/actor-sagas.runner"
      testProbe.expectMsg(500 millis, expectedResponse)
    }
  }
}
