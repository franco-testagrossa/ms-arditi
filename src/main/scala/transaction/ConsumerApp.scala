package transaction

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer }
import akka.{ Done, actor }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object ConsumerApp extends App {

  val system: actor.ActorSystem = ActorSystem("ClusterArditi")

  implicit val mat: Materializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  val consumerSettings =
    ConsumerSettings(
      ConfigFactory.load().getConfig("akka.kafka.consumer"),
      new StringDeserializer,
      new StringDeserializer
    )
  val consume: Future[Done] = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("stage1"))
    .map { msg =>
      /*val decoded: Either[circe.Error, Payment.UpdateMovimiento] =
        decode[OldModel](msg.value().drop(7)) map { decoded =>
          Adaptor.toUpdateMovimiento(decoded)
        }
      println(decoded)*/
      println(msg)
    }

    .runWith(Sink.ignore)

  consume onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }

}
