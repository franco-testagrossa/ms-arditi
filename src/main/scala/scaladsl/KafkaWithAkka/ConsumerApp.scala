package scaladsl.KafkaWithAkka

/*
import akka.actor.Props
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ Subscriptions }
import akka.stream.scaladsl.Sink

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }
import scaladsl.KafkaWithAkka.exampleBoundedContext.HowToCookAnEgg.runner.{ TypedWaiter, UntypedWaiter }
import akka.{ Done }
import eggs.domain.EggStyle.HardBoiled

object ConsumerApp extends App {
  import common.kafka.KafKaConsumerDefaults._
  import akka.actor.typed.scaladsl.adapter._

  val typedActorExample = system.spawn(TypedWaiter.behavior, "TypedWaiter")
  val untypedActorExample = system.actorOf(Props[UntypedWaiter], "Waiter")

  val consume: Future[Done] = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .map(m => m.value() match {
      case "cook me an egg now!" =>
        typedActorExample ! HardBoiled
      case msg => untypedActorExample ! msg
    })
    .runWith(Sink.ignore)

  consume onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }

}
*/