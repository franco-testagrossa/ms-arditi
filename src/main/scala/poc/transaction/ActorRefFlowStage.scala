package poc.transaction

import akka.actor.ActorRef
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import poc.model.TX
import poc.ddd.{Response, _}

import scala.reflect.ClassTag


/**
  * Sends the elements of the stream to the given `ActorRef`.
  * The target actor can emit elements at any time by sending a `StreamElementOut(elem)` message, which will
  * be emitted downstream when there is demand.
  */
class ActorRefFlowStage[A <: poc.ddd.Command, B <: poc.ddd.Response : ClassTag](private val flowActor: ActorRef) extends
  GraphStage[FlowShape[TX[A], TX[B]]]
{

  import ActorRefFlowStage._
  import TX._

  val in: Inlet[TX[A]] = Inlet("ActorFlowIn")
  val out: Outlet[TX[B]] = Outlet("ActorFlowOut")
  var tx: TX[_] = _ // beware

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    def stageActorReceive(messageWithSender: (ActorRef, Any)): Unit = {
      def onElementOut(elemOut: B): Unit = {
        val elem: TX[B] = transactionalMessageFunctor.map(tx) { a =>
          elemOut
        }
        emit(out, elem)
      }

      messageWithSender match {
        case (_, StreamElementOut(elemOut: B)) =>
          onElementOut(elemOut)
          pullIfNeeded()
          completeStageIfNeeded()

        case (actorRef, unexpected) =>
          failStage(new IllegalStateException(s"Unexpected message: `$unexpected` received from actor `$actorRef`."))
      }
    }

    private lazy val self = getStageActor(stageActorReceive)

    setHandler(in, new InHandler {

      override def onPush(): Unit = {
        val elementIn = grab(in)
        tx = elementIn // beware
        tellFlowActor(StreamElementIn(elementIn.record.value()))
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pullIfNeeded()
      }
    })

    private def pullIfNeeded(): Unit = {
      if(!hasBeenPulled(in)) {
        tryPull(in)
      }
    }

    private def completeStageIfNeeded(): Unit = {
      if(isClosed(in)) {
        //Complete stage when in is closed, this might happen if onUpstreamFinish is called when still expecting an ack.
        this.completeStage()
      }
    }

    private def tellFlowActor(message: Any): Unit = {
      flowActor.tell(message, self.ref)
    }
  }

  override def shape: FlowShape[TX[A], TX[B]] = FlowShape(in, out)

}

object ActorRefFlowStage {
  import poc.ddd._

  case class StreamElementIn[A <: Command](element: A) extends Command {
    override def aggregateRoot: String = element.aggregateRoot
    override def deliveryId: Long = element.deliveryId
  }
  case class StreamElementOut[A](element: Response)


  def fromActor[A <: Command,B <: Response : ClassTag](actorRef: ActorRef): ActorRefFlowStage[A, B] =
    new ActorRefFlowStage[A, B](actorRef)
}