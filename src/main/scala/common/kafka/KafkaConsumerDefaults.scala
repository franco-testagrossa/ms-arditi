package common.kafka

import HowToCookAnEgg.runner.{TypedWaiter, UntypedWaiter}
import akka.{Done, actor}
import akka.actor.Props
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import common.cqrs.utils.ReadOrientedActorSystem
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

trait Consumer extends ReadOrientedActorSystem{


  val system: actor.ActorSystem = createActorSystem()

  private implicit val mat: Materializer = ActorMaterializer()(system)
  private implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val config = ConfigFactory.load()
  private val consumerConfig = config.getConfig("akka.kafka.consumer")
  private val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  def start() = {


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
        println("Done"); system.terminate()
      case Failure(err) => println(err.toString); system.terminate()
    }
  }

}
