package model

package object ddd {

  trait Command extends Product with Serializable {
    def aggregateRoot: String

    def deliveryId: BigInt
  }

  trait Query {
    def aggregateRoot: String
  }

  trait Response extends Product with Serializable {
    def deliveryId: BigInt
  }

  trait Event extends Product with Serializable {
    def name: String
  }

  final case class GetState(aggregateRoot: String) extends Query

}
