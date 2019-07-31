package scaladsl.free

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import scaladsl.free.QuickExample.{as, future}
import scalaz.concurrent.Task
import scalaz.{Free, Monad, ~>}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object FreeExample extends App {
  // Algebra
  sealed trait IO[A]
  case class PrintLn(str: String) extends IO[Unit]
  case class GetStr() extends IO[String]

  type IOF[A] = Free[IO, A]

  trait IOService {
    def printLn(str: String): IOF[Unit] =
      Free.liftF(PrintLn(str))

    def getStr(): IOF[String] =
      Free.liftF(GetStr())
  }
  object IOServiceImpl extends IOService

  // Interpreter
  trait IOServiceInterpreter[M[_]] {
    def apply[A](action: IOF[A]): M[A]
  }

  object IOServiceTryInterpreter
    extends IOServiceInterpreter[Task] {

    val step: IO ~> Task = new (IO ~> Task) {
      override def apply[A](fa: IO[A]): Task[A] =
        fa match {
          case PrintLn(str) => Task {
              Console.println(str)
                .asInstanceOf[A]
            }
          case GetStr() => Task.now {
              scala.io.StdIn.readLine()
                .asInstanceOf[A]
            }
        }
    }

    override def apply[A](action: IOF[A]): Task[A] =
      action.foldMap(step)
  }

  // Main - Program
  import IOServiceImpl._
  val printYourName: IOF[Unit] = for {
    _    <- printLn("print your name")
    name <- getStr()
    _    <- printLn(name)
  } yield ()

  val value: Task[Unit] =
    IOServiceTryInterpreter.apply(printYourName)

  value.unsafePerformSync
}

object QuickExample extends App {
  val as = ActorSystem("ClusterArditi")
  private val echoActor: ActorRef = as.actorOf(Props[EchoActor], "QuickActor")

  private val unitA: Unit = echoActor ! "A"
  private val unitB: Unit = echoActor ! "B"
  private val unitC: Unit = echoActor ! "C"

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val ec = as.dispatcher
  implicit val timeot = akka.util.Timeout(3 seconds)
  private val future: Future[String] = (echoActor ? "D").mapTo[String]
  private val result: String = Await.result(future, 3 seconds)

  println(result)
}

class EchoActor extends Actor with ActorLogging {
  override def receive: Receive = {
    case str =>
      log.info("QuickActorRef received {}", str)
      sender() ! s"QuickActorRef SENDER received $str"
  }
}

object ActorDSL extends App {
  type ActorRefF = ActorRef
  // Algebra
  sealed trait ActorF[A]
  object ActorF {
    case class Tell(actorRef: ActorRefF, message: Any, sender: ActorRefF = Actor.noSender) extends ActorF[Unit]
    case class Ask[A](actorRef: ActorRefF, message: Any, timeout: Timeout, sender: ActorRefF = Actor.noSender) extends ActorF[A]

    trait ActorFDSL {
      type Actor[A] = Free[ActorF, A]
      def tell(actorRef: ActorRefF, message: Any)(implicit sender: ActorRefF = Actor.noSender): Actor[Unit] =
        Free.liftF(Tell(actorRef: ActorRefF, message, sender))
      def ask[A](actorRef: ActorRefF, message: Any)(implicit timeout: Timeout, sender: ActorRefF = Actor.noSender): Actor[A] =
        Free.liftF(Ask(actorRef: ActorRefF, message, timeout, sender))
    }
    object ActorFDSL extends ActorFDSL
  }

  // Interpreter
  trait ActorFInterpreter[M[_]] {
    val step: ActorF ~> M
    def apply[A](action: Free[ActorF, A]): M[A]
  }
  object ActorFInterpreter {
    import scalaz._
    import Scalaz._
    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global // should be injected (Kleisli)

    object ActorFInterpreterPROD extends ActorFInterpreter[Future] {

      override val step: ActorF ~> Future = new (ActorF ~> Future) {
        override def apply[A](fa: ActorF[A]): Future[A] = fa match {
          case ActorF.Tell(actorRef, message, sender) =>
            Future.successful((actorRef ! message).asInstanceOf[A])
          case ActorF.Ask(actorRef, message, timeout, sender) =>
            import akka.pattern.ask
            (actorRef ? message)(timeout).map(_.asInstanceOf[A])
        }
      }

      override def apply[A](action: Free[ActorF, A]): Future[A] =
        action.foldMap(step)
    }
  }


  // Main
  object TheMain {
    import scala.concurrent.duration._
    import ActorF.ActorFDSL._
    import ActorFInterpreter._

    def run(): Unit = {
      val as = ActorSystem("ClusterArditi")
      implicit val ec = as.dispatcher
      implicit val timeot = akka.util.Timeout(3 seconds)

      val echoActor: ActorRef = as.actorOf(Props[EchoActor], "QuickActor")
      val result: Free[ActorF, String] = for {
        _   <-  tell(echoActor, "A")
        _   <-  tell(echoActor, "B")
        _   <-  tell(echoActor, "C")
        str <-  ask[String](echoActor, "C")
      } yield str

      val eventualString = ActorFInterpreterPROD(result)


      val value: String = Await.result(eventualString, 3 seconds)
      println(value)

    }
  }

  TheMain.run()
}

object AkkaStreamFlow extends App {
  import akka.actor.ActorSystem

  import ModelFlow._
  import ActorPersistentFlow._
  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val system = ActorSystem("ClusterArditi")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val aggregateId = "AggregateId"
  val addObligacion = (actorFlow: ActorRef) =>
    Flow[Obligacion]
      .map { obligacion =>
        implicit val askTimeout = Timeout(30 seconds)
        (actorFlow ? AddObligacion(aggregateId, obligacion)).mapTo[ObligacionAdded]
      }

  val actorFlow = system.actorOf(ActorPersistentFlow.props(), aggregateId)

  Source.fromIterator(() => Iterator(
    Obligacion("1"),
    Obligacion("2"),
    Obligacion("3")
  ))
  .via(addObligacion(actorFlow))
  .runWith(Sink.foreach(println))

  system.whenTerminated.onComplete {
    case Failure(exception) =>
      println(s"System finished with failure $exception")
    case Success(value) =>
      println(s"System finished with success $value")
  }
}

object ModelFlow {
  case class Obligacion(id: String)
}
object ActorPersistentFlow {
  import ModelFlow._

  def props(): Props = Props(new ActorPersistentFlow)
  // CMD
  sealed trait CMD
  case class AddObligacion(id: String, o: Obligacion) extends CMD
  case class GetObligacion(id: String) extends CMD

  sealed trait EVT
  case class ObligacionAdded(o: Obligacion) extends EVT
}

class ActorPersistentFlow extends PersistentActor with ActorLogging {
  import ModelFlow._
  import ActorPersistentFlow._

  override def persistenceId: String = self.path.name
  private var obligaciones: Option[Set[Obligacion]] = None

  // START Actor Hooks LifeCycle
  def logActor(msg: String): Unit = log.info("[{}] ActorPersistentFlow {}", persistenceId, msg)
  override def preStart(): Unit = {
    logActor("[PreStart]")
    super.postStop()
  }
  override def postStop(): Unit = {
    logActor("[PostStop]")
    super.postStop()
  }
  override def postRestart(reason: Throwable): Unit = {
    logActor(s"[PostRestart] with throwable ${reason.getMessage}")
    super.postRestart(reason)
  }
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    logActor(s"[PreRestart] with throwable ${reason.getMessage} and msg: $message")
    super.preRestart(reason, message)
  }
  // END   Actor Hooks LifeCycle

  override def receiveCommand: Receive = {
    case AddObligacion(id, o) =>
      logActor(s"received obligacion $o")
      persist(ObligacionAdded(o)) { evt =>
        logActor(s"persisted evt $evt")
        logActor(s"changing state $obligaciones")
        updateState(evt)
        logActor(s"changed state $obligaciones")
      }
    case Passivate =>
      context.stop(self)
    case msg =>
      logActor(s"received Unknown message $msg")
  }

  def updateState(ev: EVT): Unit = ev match {
    case ObligacionAdded(o) =>
      obligaciones = obligaciones match {
        case Some(set) => Some(set + o)
        case None => Some(Set(o))
      }
  }

  override def receiveRecover: Receive = {
    case evt: EVT => updateState(evt)
  }
}
