package utils.generators

import model.payment.PaymentActor.{Movimiento, UpdateMovimiento}

object Model {
  def updatePayment(
                        paymentId: String,
                        productId: String,
                        personId: String,
                        aggregateRoot: String,
                        deliveryId: BigInt,
                      ) = UpdateMovimiento(
    aggregateRoot,
    deliveryId,
    movimiento = exampleMovimiento(paymentId, productId, personId)
  )

  def exampleMovimiento(
                         paymentId: String,
                         productId: String,
                         personId: String) = Movimiento(
    paymentId,
    productId,
    personId,
    money = 1,
  )


}
