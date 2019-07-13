package scaladsl.KafkaWithAkka.HowToCookAnEgg.runner

import scaladsl.KafkaWithAkka.HowToCookAnEgg.runner.TypedWaiter.{ WaiterTalk, prepareEgg }
import akka.actor.{ Actor, ActorLogging }
import eggs.api.FoodPrepApi
import eggs.domain.EggStyle
import eggs.domain.EggStyle._

import scala.concurrent.ExecutionContext.Implicits.global

class UntypedWaiter extends Actor
  with ActorLogging
  with FoodPrepApi
  with Waiter
  with Adaptor {

  def receive: Actor.Receive = {

    case eggStyle: String =>
      prepareEgg(toEggStyle(eggStyle))
        .map(done =>
          log.info(DumbWaiterTalk(eggStyle)))

    case _ => log.error("I could not understand you, sire")

  }
  //sender ! self.path.toString

}
