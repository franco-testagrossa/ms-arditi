package HowToLoadAMusket.service.Messaging

import HowToLoadAMusket.service.MagicTypedLand.ActorPersistance.MusketEntity
import akka.Done
import akka.actor.Props
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import common.kafka.KafkaConsumerDefaults

import scala.concurrent.Future
import scala.util.{Failure, Success}

object KafkaConsumer extends App {
  import KafkaConsumerDefaults._

  val untypedActorExample = system.actorOf(Props[MusketEntity], "Waiter")

  val consume: Future[Done] = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .map(m => m.value() match {
      case "" => untypedActorExample ! "msg"
    })
    .runWith(Sink.ignore)

  consume onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }

}
