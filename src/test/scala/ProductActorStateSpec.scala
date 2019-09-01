import model.product.ProductActor
import model.product.ProductActor.{Payment, PaymentUpdated}
import java.time.LocalDateTime
import org.scalatest.WordSpec

class ProductActorStateSpec extends WordSpec {

  "StateProduct" should {
    "calculate person report" in {
      def printState(s: ProductActor.StateProduct, payment: Payment): Unit = {
        println(s.personReport(payment))
        println(s.balance)
      }

      val payment = Payment("1", "1", 100.0)
      val s0 = ProductActor.StateProduct.init()
      printState(s0, payment)
      val s1 = s0 + PaymentUpdated(payment) // u1
      printState(s1, payment)
      val s2 = s1 + PaymentUpdated(payment) // u2
      printState(s2, payment)
      val s3 = s2 + PaymentUpdated(payment.copy(personId = "2")) // u3
      printState(s3, payment)
      val s4 = s3 + PaymentUpdated(payment.copy(personId = "2")) // u3
      printState(s4, payment)
      val s5 = s4 + PaymentUpdated(payment.copy(personId = "3")) // u3
      printState(s5, payment)
    }
  }
}
