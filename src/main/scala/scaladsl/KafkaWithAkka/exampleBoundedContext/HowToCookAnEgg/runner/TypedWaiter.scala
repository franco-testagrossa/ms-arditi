package scaladsl.KafkaWithAkka.HowToCookAnEgg.runner

import akka.actor.typed.scaladsl.Behaviors
import eggs.api.FoodPrepApi
import eggs.domain.EggStyle

import scala.concurrent.ExecutionContext.Implicits.global

object TypedWaiter extends FoodPrepApi with Waiter {

  val behavior: Behaviors.Receive[EggStyle] =
    Behaviors.receive { (context, eggStyle) =>
      prepareEgg(eggStyle)
        .map(done =>
          context.log.info(WaiterTalk(eggStyle.getClass.getSimpleName)))
      Behaviors.same

    }
}

