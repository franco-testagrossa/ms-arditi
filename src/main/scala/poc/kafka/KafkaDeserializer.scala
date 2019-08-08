package poc.kafka

import java.io.{ ByteArrayInputStream, ObjectInputStream }
import java.util
import org.apache.kafka.common.serialization.Deserializer

class KafkaDeserializer[T] extends Deserializer[T] {

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def deserialize(topic: String, data: Array[Byte]): T =
    if (data == null) null.asInstanceOf[T]
    else {
      val byteIn = new ByteArrayInputStream(data)
      val objIn = new ObjectInputStream(byteIn)
      val obj = objIn.readObject().asInstanceOf[T]
      byteIn.close()
      objIn.close()
      obj
    }

  override def close(): Unit = {
    // nothing to do
  }
}
