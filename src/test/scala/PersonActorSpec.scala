
import java.time.LocalDateTime

import akka.RestartActorSupervisorFactory
import akka.actor.Kill
import akka.util.Timeout
import model.ddd.GetState
import model.person.PersonActor
import model.person.PersonActor.Product
import utils.ClusterArditiSpec

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PersonActorSpec extends ClusterArditiSpec {

  "The Person" should {

    val expectedBalance = 2000.0

    "should be update, be killed, and restarted it with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory
      val person = supervisor.create(PersonActor.props(), "Person-1")

      person ! GetState("1")
      person ! PersonActor.UpdateProduct(
        aggregateRoot = "1",
        deliveryId = 1L,
        product = Product(
          productId = "1",
          personId = "1",
          balanceProduct = expectedBalance
        ))

      person ! Kill
      Thread.sleep(200)

      person ! GetState("1")

      val one = BigInt(1)
      within(3 seconds) {
        expectMsgPF() {
          case PersonActor.State(balance, products)
            if balance == 0 && products.isEmpty => true
        }
        expectMsgPF() {
          case PersonActor.UpdateSuccess(_, `one`, _) => true
        }
        expectMsgPF() {
          case PersonActor.State(balance, products)
            if balance == expectedBalance => true
        }
      }

    }

    "should update person with sharding" in {
      val product = PersonActor.start

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).flatMap {
          deliveryId =>
            (1 to 2).map { productId =>
              product ? PersonActor.UpdateProduct(
                aggregateRoot = "1",
                deliveryId = deliveryId,
                product = Product(
                  productId = productId.toString,
                  personId = "1",
                  balanceProduct = expectedBalance
                ))
            }

        })

      Await.result(transactions, 10 second)

      val state = (product ? GetState("1")).mapTo[PersonActor.State]
      state.foreach { a =>
        println(s"Expected balance is: ${expectedBalance * N * 2}, and balance is ${a.balance}")
        assert(a.balance == expectedBalance * N * 2)
        assert(
          a.products.collect {
            case (_, o@Product(_, "1", balance)) if balance == expectedBalance * N => o
          }.size == 2
        )
      }
    }
  }
}
