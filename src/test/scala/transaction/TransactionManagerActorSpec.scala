package transaction

import org.scalatest.{ BeforeAndAfterAll, WordSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ TestKit, TestProbe }
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.actor.Props
import account._
import domain.Account

class TransactionManagerActorSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {

  import TransactionManagerActor._

  def this() = this(utils.ClusterArditiSystem.system)

  "A TransactionManagerActorSpec" should {
    "demonstrate compensation in case of insufficient balance" in {
      val testProbe = TestProbe()
      val diana = system.actorOf(AccountActor.props(active  = true, balance = 500), "Diana")
      val arthur = system.actorOf(AccountActor.props(active  = true, balance = 10000), "Arthur")
      val tm = system.actorOf(TransactionManagerActor.props(), "tm2")
      diana.!("print")(testProbe.ref)
      arthur.!("print")(testProbe.ref)
      tm ! TransferMoney(123456L, "Diana", "Arthur", 1000L)
      Thread.sleep(5000)
      diana.!("print")(testProbe.ref)
      arthur.!("print")(testProbe.ref)
      val expectedResponse: String = "akka://ActorRunnerSpec/user/actor-runner"
      // testProbe.expectMsg(500 millis, expectedResponse)
    }
    "demonstrate compensation in case of receiver account is inactive" in {
      val testProbe = TestProbe()
      val barry = system.actorOf(AccountActor.props(active  = true, balance = 500), "Barry")
      val victor = system.actorOf(AccountActor.props(active  = false, balance = 10000), "Victor")
      val tm = system.actorOf(TransactionManagerActor.props(), "tm3")
      barry.!("print")(testProbe.ref)
      victor.!("print")(testProbe.ref)
      tm ! TransferMoney(123456L, "Barry", "Victor", 100L)
      Thread.sleep(5000)
      barry.!("print")(testProbe.ref)
      victor.!("print")(testProbe.ref)

      val expectedResponse: String = "akka://ActorRunnerSpec/user/actor-runner"
      // testProbe.expectMsg(500 millis, expectedResponse)
    }
  }
}
