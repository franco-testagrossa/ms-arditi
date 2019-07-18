package scaladsl.KafkaWithAkka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object ProducerApp extends App {
  import common.kafka.KafkaProducerActorAsWorker._

  val value = "cook me an egg now! ... hello from Kafka Producer, my man!"
  val produce: Future[Done] =
    Source(1 to 5)
      .map(index => {
        println(s"Publishing ${value} to topic test")
        new ProducerRecord[String, String]("test", value)
      })
      .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}
