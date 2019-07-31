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
import scalaz.{Free, \/}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class WithEventLogSpec extends ClusterArditiSpec {

  "PersistentEntity DSL" should {
    import domainDrivenDesign.boundedContexts.bank.modules.account.algebra.rules.interpreter.MyEntity._
    import akka.pattern.ask
    import scala.concurrent.duration._
    import akka.util.Timeout

    implicit val ec: ExecutionContextExecutor = system.dispatcher
    implicit val timeout: Timeout = Timeout(3 seconds)
    "succeed" in {
      //val testProbe = TestProbe()
      //val supervisor = new RestartActorSupervisorFactory

      // yo quiero testear un actor ? no
      // quiero testear la interfaz (contrato que presenta el actor)

      trait InterfazAggregate
      trait InterfazAggregateA extends InterfazAggregate {
        def cmd1: String
        def cmd2: Int
      }

      object InterfazAggregateAImpl extends InterfazAggregateA {
        override def cmd1: String = "???"
        override def cmd2: Int = 0
      }

      InterfazAggregateAImpl.cmd1


      abstract class Prod extends PersistentActor {
        import InterfazAggregateAImpl._
      }




      val aggregate: akka.actor.ActorRef =
        childActorOf(MyEntity.props(), "Account")

      aggregate ! Run("1")
      aggregate ! Run("2")
      aggregate ! Run("3")


      val result = for {
        a <- (aggregate ? Run("1")).mapTo[Response[Account]]
        b <- (aggregate ? Run("2")).mapTo[Response[Account]]
        c <- (aggregate ? Run("3")).mapTo[Response[Account]]
      } yield (a, b, c)
      Thread.sleep(1000)
      result.onComplete {
        case Failure(exception) =>
          println(s"Failure($exception)")
        case Success(value) =>
          println(s"Success($value)")
      }
      Thread.sleep(1000)
    }
  }

  "Testing only the Algebra" should {
    "succeed" in {
      import MyEntity._
      var state: State[Account] = MyState(Account("0", "MyEntity", DateTime.now))
      // use the state monad to alter the state

      /*
      val flow = for {
        ev1 <- cmd1()
        ev2 <- cmd2()
        ev3 <- cmd3()
      } yield ()
       */

      

      val events = Runned("0") :: Runned("1") :: Runned("3") :: Nil
      state = events.map(identity[Event[Account]]).foldLeft(state)(_+_)
      println(state.aggregate)
    }
  }

  /*
  1 Cmd => Validar el estado State
    luego si el comando es valido de ser procesado
    como se relacciona el commaand con la bussiess rule ?

   */

  // "WithEventLog" ignore {
  //    object withEventLog extends App {
  //     import AccountRulesWithMockDB._
  //     import AccountRulesV1._
  //
  //     val result: Task[Account] = apply(comp)
  //     val complete: Account = result.unsafePerformSync
  //     val events = allEvents
  //
  //     events.map(
  //       events => println(
  //         events.reverse.mkString("\n")))
  //   }
  //   "do something" in {
  //     withEventLog.main(Array())
  //   }
  // }
}



