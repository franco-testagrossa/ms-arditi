package poc

import scala.util.Try

package object model {
  case class ModelRequest()
  case class ModelResponse()

  case class DataRecord()
  object DataRecord {
    def fromByteArray(bytes: Array[Byte]): Try[ModelRequest] = Try(ModelRequest())
  }
}
