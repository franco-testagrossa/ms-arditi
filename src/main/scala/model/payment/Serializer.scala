package model.payment

import io.circe.{Decoder, Encoder, HCursor, Json}
import model.product.ProductActor
import model.product.ProductActor.Payment
import model.payment.PaymentActor.Movimiento

object Serializer {

  import kafka.SerializerUtils._

  implicit val encoderMovimiento: Encoder[Movimiento] = (a: Movimiento) => Json.obj(
    ("personId", a.personId),
    ("productId", a.productId),
    ("money", a.money),
    ("paymentId", a.paymentId),
  )

  implicit def encoderPaymentUpdateMovimiento: Encoder[PaymentActor.UpdateMovimiento] =
    (a: PaymentActor.UpdateMovimiento) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("movimiento", encoderMovimiento(a.movimiento))

    )

  implicit val encoderPaymentUpdateSuccess: Encoder[PaymentActor.UpdateSuccess] =
    (a: PaymentActor.UpdateSuccess) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("movimiento", encoderMovimiento(a.movimiento))
    )

  implicit val decoderPaymentUpdateUpdateSuccess: Decoder[PaymentActor.UpdateSuccess] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      movimiento <- c.downField("movimiento").as[Movimiento]

    } yield PaymentActor.UpdateSuccess(
      aggregateRoot,
      deliveryId,
      movimiento
    )
  }

  implicit val decoderPayment: Decoder[Payment] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[String]
      balancePayment <- c.downField("balancePayment").as[BigDecimal]

    } yield Payment(
      aggregateRoot,
      deliveryId,
      balancePayment = balancePayment
    )
  }

  implicit val decoderUpdatePayment: Decoder[ProductActor.UpdatePayment] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      payment <- c.downField("movimiento").as[Payment](decoderPayment)

    } yield ProductActor.UpdatePayment(
      aggregateRoot,
      deliveryId,
      payment
    )
  }

  implicit val decoderMovimiento: Decoder[Movimiento] = (c: HCursor) => {
    for {
      personId <- c.downField("personId").as[String]
      productId <- c.downField("productId").as[String]
      money <- c.downField("money").as[BigDecimal]
      paymentId <- c.downField("paymentId").as[String]

    } yield Movimiento(
      personId = personId,
      productId = productId,
      money = money,
      paymentId = paymentId
    )
  }
}