package runner

import org.scalatest.{ BeforeAndAfterAll, WordSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe }
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.Props

class ActorRunnerSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(utils.ClusterArditiSystem.system)

  "An ActorRunner" should {
    "return its path for every msg sent" in {
      val testProbe = TestProbe()
      val actorRunner = system.actorOf(Props[ActorRunner], "actor-runner")
      actorRunner.!("msg")(testProbe.ref)
      val expectedResponse: String = "akka://ClusterArditi/user/actor-runner"
      testProbe.expectMsg(500 millis, expectedResponse)
    }
  }
}
