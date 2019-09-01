package model.person

import java.time.LocalDateTime

import akka.ShardedEntity
import akka.actor.{ ActorLogging, Props }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import model.ddd.{ GetState, _ }

class PersonActor extends PersistentActor with ActorLogging {

  import PersonActor._

  private var state: State = State.init()

  override def receiveCommand: Receive = {

    case UpdateProduct(aggregateRoot, deliveryId, product) =>
      val evt = ProductUpdated(product)
      persist(evt) { e =>
        state += e
        val response = UpdateSuccess(aggregateRoot, deliveryId, product)
        sender() ! response
        val logMsg = "[{}][New]"
        log.info(logMsg, response)
      }

    case GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[{}][GetState|{}]"
      log.info(logMsg, persistenceId, state.toString)

    case other =>
      val logMsg = "[{}][WrongMsg|{}]"
      log.info(logMsg, persistenceId, other.toString)
  }

  override def persistenceId: String = typeName + "-" + self.path.name

  override def receiveRecover: Receive = {
    case evt: Event =>
      state += evt
    case SnapshotOffer(_, snapshot: State) =>
      state = snapshot

  }
}

object PersonActor extends ShardedEntity {

  val typeName = "Person"

  def props(): Props = Props(new PersonActor)

  final case class UpdateProduct(
    aggregateRoot: String,
    deliveryId:    BigInt,
    product:       Product

  ) extends Command

  final case class UpdateSuccess(aggregateRoot: String, deliveryId: BigInt, product: Product) extends Response

  final case class ProductUpdated(product: Product) extends Event {
    def name: String = "ProductUpdated"
  }

  // State
  final case class Product(
    productId:      String,
    personId:       String,
    balanceProduct: BigDecimal
  )

  final case class State private (
    balance:  BigDecimal,
    products: Map[String, Product]
  ) {
    def +(event: Event): State = event match {
      case ProductUpdated(product: Product) =>
        copy(
          balance  = calculateBalance(product),
          products = updateProducts(product)
        )
    }

    def calculateBalance(o: Product): BigDecimal = balance + o.balanceProduct // is a delta with +- sign
    def updateProducts(o: Product): Map[String, Product] = {
      val balanceDelta = o.balanceProduct
      products.get(o.productId).map { ob =>
        val oldBalance = ob.balanceProduct
        val newProduct = o.copy(balanceProduct = balanceDelta + oldBalance)
        newProduct
      } match {
        case Some(newProduct) => products + (newProduct.productId -> newProduct)
        case None => products + (o.productId -> o)
      }
    }
  }

  object State {
    def init(): State = new State(0, Map.empty[String, Product])
  }

}
