package transaction

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.pattern.ask
import akka.util.Timeout
import config.AppConfig
import model.ddd.GetState
import model.payment.PaymentActor
import model.person.PersonActor
import model.person.PersonActor.Product
import model.product.ProductActor
import model.product.ProductActor.Payment

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class TransactionFlow(config_ : AppConfig)(implicit system: ActorSystem) {

  def controlGraph(
                    payment: ActorRef,
                    product: ActorRef,
                    person: ActorRef
                  )
                  (implicit t: Timeout, ec: ExecutionContext,
                   consumer: ConsumerSettings[String, String],
                   producer: ProducerSettings[String, String]) = {

    import Transaction.□

    type A2 = PaymentActor.UpdateMovimiento
    type A3 = PaymentActor.UpdateSuccess
    type A4 = PersonActor.UpdateProduct
    type A5 = PersonActor.UpdateSuccess
    type A6 = PersonActor.State

    implicit def toFutureSeq[A](a: Future[A]): Future[Seq[A]] = a.map(Seq(_))


    def updatePayment(a: A2): Future[A3] =
      (payment ? a).mapTo[A3]

    def updateProduct(a: A3): Future[Seq[PersonActor.UpdateProduct]] = {
      (product ? toUpdatePayment(a))
        .mapTo[ProductActor.UpdateSuccess]
        .map(b => toUpdateProduct_(b).toSeq)
    }

    def updatePerson(a: A4): Future[A5] =
      (person ? a).mapTo[A5]

    def getPersonState(a: A5): Future[A6] =
      (person ? GetState(a.aggregateRoot)).mapTo[A6]

    import model.payment.Serializer._
    import model.person.Serializer._
    import model.product.Serializer._

    □[A2, A3]("stage1", "updatePayment", "test", updatePayment)
    □[A3, A4]("updatePayment", "updateProduct", "test", updateProduct)
    □[A4, A5]("updateProduct", "updatePerson", "test", updatePerson)
    □[A5, A6]("updatePerson", "getPersonState", "test", getPersonState)

  }

  def toUpdatePayment(o: PaymentActor.UpdateSuccess) =
    ProductActor.UpdatePayment(
      aggregateRoot = o.movimiento.productId,
      deliveryId = o.deliveryId,
      payment = Payment(
        paymentId = o.aggregateRoot,
        personId = o.movimiento.personId,
        balancePayment = o.movimiento.money,
      )
    )

  def toUpdateProduct_(o: ProductActor.UpdateSuccess): immutable.Iterable[PersonActor.UpdateProduct] =
    o.persons.map { case (sid, balance) => toUpdateProduct(sid, balance)(o) }

  def toUpdateProduct(personId: String, balanceProduct: BigDecimal): ProductActor.UpdateSuccess => PersonActor.UpdateProduct = { o: ProductActor.UpdateSuccess =>
    PersonActor.UpdateProduct(
      aggregateRoot = personId,
      deliveryId = o.deliveryId,
      product = Product(
        productId = o.aggregateRoot,
        personId = personId,
        balanceProduct = balanceProduct
      )
    )
  }

  private def transactionalId: String = java.util.UUID.randomUUID().toString
}
