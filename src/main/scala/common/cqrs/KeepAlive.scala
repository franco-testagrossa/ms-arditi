package common.cqrs

import akka.actor.{ Actor, ActorLogging, Props, Timers }
import akka.cluster.sharding.ClusterSharding

object KeepAlive {
  case object ProbeEventProcessors
  case object Ping
  case object Pong

  def props(eventProcessorId: String): Props = Props(new KeepAlive(eventProcessorId))

}

class KeepAlive(typeName: String) extends Actor with ActorLogging with Timers {
  import KeepAlive._

  private val shardRegion = ClusterSharding(context.system).shardRegion(typeName)
  private val settings = Settings(context.system)

  override def receive: Receive = {
    case ProbeEventProcessors =>
      for (eventProcessorN <- 0 until settings.eventProcessorSettings.parallelism) {
        val eventProcessorId: String = s"${settings.eventProcessorSettings.tagPrefix}$eventProcessorN"
        shardRegion ! EventProcessorWrapper.EntityEnvelope(eventProcessorId, Ping)
      }
    case Pong =>
    //log.info(s"EventProcessor living at ${sender().path}")
  }

  override def preStart(): Unit = {
    super.preStart()
    timers.startPeriodicTimer("keep-alive", ProbeEventProcessors, settings.eventProcessorSettings.keepAliveInterval)
  }

}
