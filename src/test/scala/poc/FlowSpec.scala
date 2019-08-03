package poc

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import sagas.utils.ClusterArditiSpec

import scala.concurrent.duration._

class FlowSpec extends ClusterArditiSpec {
  class Mapper extends Actor with ActorLogging {
    // START Actor Hooks LifeCycle
    def logActor(msg: String): Unit = log.info("Mapper {}", msg)
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

    override def receive: Receive = {
      case n: Int =>
        log.info("Mapper received {}", n)
        sender() ! (n * 5)
    }
  }
  "FlowSpec" should {
    "work" in {
      implicit val m = ActorMaterializer()
      implicit val t = Timeout(30 seconds)
      val mapper = system.actorOf(Props(new Mapper))

      val source = Source(1 to 100)
//      val flow = Flow[Int].map(_ * 5)
      val flow = Flow[Int].ask(mapper)
      val sink = Sink.foreach(println)

      source.via(flow).runWith(sink)

      Thread.sleep(5000)


    }
  }

}
