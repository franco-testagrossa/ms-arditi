package sagas.account

import akka.actor.{Kill, PoisonPill, Props}
import akka.testkit.TestProbe
import sagas.runner.ActorRunner
import sagas.transaction.TransactionManagerActor
import sagas.transaction.TransactionManagerActor.TransferMoney
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.concurrent.duration._

class AccountActorSpec extends ClusterArditiSpec {
  import AccountActor._

  "The Aggregate AccountActor" should {
    "demonstrate balance amount increasing" in {
      val testProbe = TestProbe()
      val supervisor = new RestartActorSupervisorFactory
      val diana = supervisor.create(AccountActor.props(active = true, balance = 500), "Diana")
      diana.!("print")(testProbe.ref)
      diana.!(GetState)(testProbe.ref)
      diana.!(IncreaseAmount(100))(testProbe.ref)
      diana ! Kill
      Thread.sleep(1000)
      diana.!(GetState)(testProbe.ref)
      diana.!("print")(testProbe.ref)
      // testProbe.expectMsg(500 millis, expectedResponse)
      within(10 seconds) {
        testProbe.expectMsgPF() {
          case state: AccountState =>
            println(state)
        }
        testProbe.expectMsgPF() {
          case state: ConfirmAmountIncreased.type =>
            println(state)
        }
        testProbe.expectMsgPF() {
          case state: AccountState =>
            println(state)
        }
      }
    }
  }

}
