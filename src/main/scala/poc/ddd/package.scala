package poc

package object ddd {
  trait Command extends Product with Serializable {
    def aggregateRoot: String
    def deliveryId: Long
  }

  trait Query { def aggregateRoot: String }

  trait Response extends Product with Serializable {
    def deliveryId: Long
  }

  trait Event extends Product with Serializable { def name: String }

}
