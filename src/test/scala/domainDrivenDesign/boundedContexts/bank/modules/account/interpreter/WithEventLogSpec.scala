package domainDrivenDesign.boundedContexts.bank.modules.account.interpreter

import akka.actor.{ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.testkit.{TestKit, TestProbe}
import domainDrivenDesign.Abstractions.{Response, _}
import scalaz.concurrent.Task
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.domain.model._
import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter.{AccountRulesV1, MyEntity}
import org.joda.time.DateTime
import org.scalatest.WordSpecLike
import sagas.account.AccountActor
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}
import scalaz.Scalaz._
import scalaz.\/

import scala.util.{Failure, Success}

class WithEventLogSpec extends ClusterArditiSpec {

  object withEventLog extends App {
    import AccountRulesWithMockDB._
    import AccountRulesV1._

    val result: Task[Account] = apply(comp)
    val complete: Account = result.unsafePerformSync
    val events = allEvents

    events.map(
      events => println(
        events.reverse.mkString("\n")))
  }

  // "WithEventLog" ignore {
  //   "do something" in {
  //     withEventLog.main(Array())
  //   }
  // }


  "PersistentEntity DSL" should {
    import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter.MyEntity._
    "succeed" in {
      val testProbe = TestProbe()
      val supervisor = new RestartActorSupervisorFactory
      val diana = supervisor.create(MyEntity.props(), "Diana")


      val aggregate = childActorOf(Props(new MyEntity), "Account")

      import akka.pattern.ask
      import scala.concurrent.duration._
      implicit val ec = system.dispatcher
      implicit val timeout = akka.util.Timeout(3 seconds)

      val result = for {
        a <- (aggregate ? Run("1")).mapTo[Response[Account]]
        b <- (aggregate ? Run("2")).mapTo[Response[Account]]
        c <- (aggregate ? Run("3")).mapTo[Response[Account]]
      } yield (a, b, c)
      result.onComplete {
        case Failure(exception) =>
          println(s"Failure($exception)")
        case Success(value) =>
          println(s"Success($value)")
      }
    }
  }
}



