
import java.time.LocalDateTime

import akka.RestartActorSupervisorFactory
import akka.actor.Kill
import akka.util.Timeout
import model.ddd.GetState
import model.payment.PaymentActor
import model.payment.PaymentActor.Movimiento
import utils.ClusterArditiSpec

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PaymentActorSpec extends ClusterArditiSpec {

  "The Payment" should {

    val expectedBalance = 200

    "should update payment, using expectMsg" in {
      val supervisor = new RestartActorSupervisorFactory
      val payment = supervisor.create(PaymentActor.props(), "Payment-1")

      payment ! GetState("1")
      payment ! PaymentActor.UpdateMovimiento(
        aggregateRoot = "1",
        deliveryId = 1L,
        movimiento = Movimiento(
          paymentId = "1",
          productId = "1",
          personId = "1",
          money = expectedBalance
        ))

      payment ! Kill
      Thread.sleep(200)

      payment ! GetState("1")
      val one = BigInt(1)
      within(3 seconds) {
        expectMsgPF() {
          case PaymentActor.StatePayment(balance, movimientos)
            if balance == 0 && movimientos.isEmpty => true
        }
        expectMsgPF() {
          case PaymentActor.UpdateSuccess(_, `one` , _) => true
        }
        expectMsgPF() {
          case PaymentActor.StatePayment(balance, movimientos)
            if balance == expectedBalance => true
        }
      }

      Thread.sleep(200)
    }

    "should update payment, using Await" in {
      val payment = PaymentActor.start

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).map(
          _ => payment ? PaymentActor.UpdateMovimiento(
            aggregateRoot = "1",
            deliveryId = N,
            movimiento = Movimiento(
              paymentId = "1",
              productId = "1",
              personId = "1",
              money = expectedBalance
            ))
        )
      )

      val pepe =   (1 to N).length * expectedBalance
      Await.result(transactions, 10 second)

      val state = (payment ? GetState("1")).mapTo[PaymentActor.StatePayment]
      state.foreach { a =>
        println(s"Expected balance is: ${expectedBalance * N}, and balance is ${a.balance} -- $pepe")
        assert(a.balance == expectedBalance * N)
      }
    }
  }
}
