package poc

import akka.actor.ActorRef
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

/**
  * Sends the elements of the stream to the given `ActorRef`.
  * The target actor can emit elements at any time by sending a `StreamElementOut(elem)` message, which will
  * be emitted downstream when there is demand.
  */
class ActorRefFlowStage[In, Out](private val flowActor: ActorRef) extends GraphStage[FlowShape[In, Out]] {

  import ActorRefFlowStage._

  val in: Inlet[In] = Inlet("ActorFlowIn")
  val out: Outlet[Out] = Outlet("ActorFlowOut")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    def stageActorReceive(messageWithSender: (ActorRef, Any)): Unit = {
      def onElementOut(elemOut: Any): Unit = {
        val elem = elemOut.asInstanceOf[Out]
        emit(out, elem)
      }

      messageWithSender match {
        case (_, StreamElementOut(elemOut)) =>
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
        tellFlowActor(StreamElementIn(elementIn))
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

  override def shape: FlowShape[In, Out] = FlowShape(in, out)

}

object ActorRefFlowStage {
  case class StreamElementIn[A](element: A)
  case class StreamElementOut[A](element: A)
}