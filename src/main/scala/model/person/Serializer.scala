package model.person

import io.circe.{Decoder, Encoder, HCursor, Json}
import model.product.ProductActor
import model.product.ProductActor.Payment
import model.payment.PaymentActor
import model.payment.PaymentActor.Movimiento
import model.person
import model.person.PersonActor.Product

object Serializer {

  import kafka.SerializerUtils._

  implicit val encoderPersonProduct: Encoder[PersonActor.Product] = (a: PersonActor.Product) => Json.obj(
    ("productId", a.productId),
    ("balanceProduct", a.balanceProduct),
    ("personId", a.personId)
  )

  implicit val encoderPersonState: Encoder[PersonActor.State] =
    (a: PersonActor.State) => Json.obj(
      ("products", Json.fromValues(a.products.map(product => encoderPersonProduct(product._2)))),
      ("balance", a.balance)
    )

  implicit val decoderPersonProduct: Decoder[PersonActor.Product] = (c: HCursor) => {
    for {
      productId <- c.downField("productId").as[String]
      personId <- c.downField("personId").as[String]
      balanceProduct <- c.downField("balanceProduct").as[BigDecimal]

    } yield person.PersonActor.Product(
      productId,
      personId,
      balanceProduct
    )
  }

  implicit val encoderPersonUpdateSuccess: Encoder[PersonActor.UpdateSuccess] =
    (a: PersonActor.UpdateSuccess) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("product", encoderPersonProduct(a.product)),
    )

  implicit val decoderPersonUpdateSuccess: Decoder[PersonActor.UpdateSuccess] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      product <- c.downField("product").as[Product]

    } yield person.PersonActor.UpdateSuccess(
      aggregateRoot,
      deliveryId,
      product
    )
  }

  import model.payment.Serializer.encoderMovimiento

  implicit def encoderPaymentUpdateMovimiento: Encoder[PaymentActor.UpdateMovimiento] =
    (a: PaymentActor.UpdateMovimiento) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("movimiento", encoderMovimiento(a.movimiento))
    )

  implicit def encoderPersonUpdateProduct: Encoder[person.PersonActor.UpdateProduct] =
    (a: person.PersonActor.UpdateProduct) => Json.obj(
      ("aggregateRoot", a.aggregateRoot),
      ("deliveryId", a.deliveryId),
      ("product", encoderPersonProduct(a.product))
    )

  implicit def decoderPersonUpdateProduct: Decoder[person.PersonActor.UpdateProduct] = (c: HCursor) => {
    for {
      aggregateRoot <- c.downField("aggregateRoot").as[String]
      deliveryId <- c.downField("deliveryId").as[BigInt]
      product <- c.downField("product").as[Product]

    } yield person.PersonActor.UpdateProduct(
      aggregateRoot,
      deliveryId,
      product
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
      balancePayment
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