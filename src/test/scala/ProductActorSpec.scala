
import akka.actor.Kill
import akka.util.Timeout
import model.ddd.GetState
import model.product.ProductActor
import model.product.ProductActor.Payment
import java.time.LocalDateTime

import akka.RestartActorSupervisorFactory
import utils.ClusterArditiSpec

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ProductActorSpec extends ClusterArditiSpec {

  "The Product" should {
    val expectedBalance = 200

    "should update product with cotitularidad" in {
      val supervisor = new RestartActorSupervisorFactory
      val product = supervisor.create(ProductActor.props(), "Product-1")

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).flatMap(deliveryId =>
          (1 to 2).map(
            paymentId =>
              product ? ProductActor.UpdatePayment(
                aggregateRoot = "1",
                deliveryId = deliveryId,
                payment = Payment(
                  paymentId = paymentId.toString,
                  personId = paymentId.toString,
                  balancePayment = expectedBalance
                ))
          )))


      product ! Kill
      Await.result(transactions, 10 second)

      val state = (product ? GetState("1")).mapTo[ProductActor.StateProduct]
      state.foreach { case ProductActor.StateProduct(balance, paymentes, _)  =>
        println(s"Expected balance is: ${expectedBalance * N * 2}, and balance is $balance")
        assert(balance == expectedBalance * N * 2)
        assert(
          paymentes.collect {
            case (_, o@Payment(_, _, balance)) if balance == expectedBalance * N => o
          }.size == 2
        )
        assert(
          paymentes.map {
            case (_, o) => o.personId
          }.size == 2
        )
      }
    }
  }

}
