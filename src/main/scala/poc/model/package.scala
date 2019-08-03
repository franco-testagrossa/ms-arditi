package poc

import akka.kafka.ConsumerMessage.TransactionalMessage

import scala.util.Try

package object model {
  type TX[A] = TransactionalMessage[String, A]

  case class ModelRequest()
  case class ModelResponse()

  case class DataRecord()
  object DataRecord {
    def fromByteArray(bytes: Array[Byte]): Try[ModelRequest] = Try(ModelRequest())
  }
}
