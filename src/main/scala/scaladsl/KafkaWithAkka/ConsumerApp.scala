package KafkaWithAkka

import akka.actor.Props
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }
import HowToCookAnEgg.runner.{ TypedWaiter, UntypedWaiter, Waiter }
import akka.{ Done, actor }
import akka.actor.typed.ActorRef
import eggs.domain.EggStyle
import eggs.domain.EggStyle.HardBoiled
import common.kafka.Consumer

object ConsumerApp extends App with Consumer{

  import akka.actor.typed.scaladsl.adapter._

  val typed = system.spawn(TypedWaiter.behavior, "TypedWaiter")
  val untyped = system.actorOf(Props[UntypedWaiter], "Waiter")

  val consume: Future[Done] = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .map(m => m.value() match {
      case "cook me an egg now!" =>
        typed ! HardBoiled
      case msg => untyped ! msg
    })
    .runWith(Sink.ignore)

  consume onComplete {
    case Success(_) =>
      println("Done"); system2.terminate()
    case Failure(err) => println(err.toString); system2.terminate()
  }

}
