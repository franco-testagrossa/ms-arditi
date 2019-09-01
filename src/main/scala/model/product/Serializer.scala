package model.product

import io.circe.{Decoder, Encoder, HCursor, Json}
import model.product
import model.product.ProductActor.Payment
import model.payment.PaymentActor
import model.payment.PaymentActor.Movimiento
import model.payment.Serializer.encoderMovimiento

object Serializer {

  import kafka.SerializerUtils._

  implicit val decoderUpdateMovimiento: Decoder[PaymentActor.UpdateMovimiento] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      movimiento <- c.downField("movimiento").as[Movimiento]

    } yield PaymentActor.UpdateMovimiento(
      aggregateRoot,
      deliveryId,
      movimiento
    )
  }

  implicit val encoderUpdateMovimiento: Encoder[PaymentActor.UpdateMovimiento] =
    (a: PaymentActor.UpdateMovimiento) => Json.obj(
      ("deliveryId", a.deliveryId),
      ("aggregateRoot", a.aggregateRoot),
      ("movimiento", encoderMovimiento(a.movimiento))
    )

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

  implicit val decoderPayment: Decoder[Payment] = (c: HCursor) => {
    for {
      personId <- c.downField("personId").as[String]
      paymentId <- c.downField("paymentId").as[String]
      balancePayment <- c.downField("balancePayment").as[BigDecimal]
    } yield Payment(
      paymentId,
      personId,
      balancePayment
    )
  }

  implicit val decoderProductUpdateSuccess: Decoder[product.ProductActor.UpdateSuccess] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      payment <- c.downField("payment").as[Payment]
      persons <- c.downField("persons").as[Map[String, BigDecimal]]

    } yield product.ProductActor.UpdateSuccess(
      aggregateRoot,
      deliveryId,
      payment,
      persons
    )
  }

  implicit val encoderPayment: Encoder[Payment] = (a: Payment) => Json.obj(
    ("paymentId", a.paymentId),
    ("balancePayment", a.balancePayment),
    ("personId", a.personId)
  )

  implicit val encoderPersons: Encoder[Map[String, BigDecimal]] = (collection: Map[String, BigDecimal]) => {
    Json.fromFields(
      collection.
        map(collection =>
          (collection._1, fromBigDecimal(collection._2))
        ))
  }

  implicit val encoderProductUpdateSuccess: Encoder[product.ProductActor.UpdateSuccess] =
    (a: product.ProductActor.UpdateSuccess) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("payment", encoderPayment(a.payment)),
      ("persons", encoderPersons(a.persons)),
    )

}
