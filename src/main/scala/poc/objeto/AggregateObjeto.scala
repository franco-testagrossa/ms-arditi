package poc.objeto

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}

class AggregateObjeto extends PersistentActor with ActorLogging {
  import AggregateObjeto._

  private val objetoId = self.path.name
  override def persistenceId: String = typeName  + "-" + objetoId

  private var state: StateObjeto = StateObjeto.init()
  private var lastDeliveredId: Long = 0L // handling ordering

  override def receiveCommand: Receive = {
    case UpdateObligacion(_, deliveryId, obligacionId, obligacion)
      if lastDeliveredId > deliveryId => // drop the message (ordering)
    case UpdateObligacion(_, deliveryId, obligacionId, obligacion) =>
      val evt = ObligacionUpdated(obligacionId, obligacion)
      persist(evt) { e =>
        state += e
        lastDeliveredId = lastDeliveredId max deliveryId
        // respond success
        val response = UpdateSuccess(deliveryId)
        sender() ! response
        val logMsg = "[AggregateObjeto|{}][ObligacionUpdated|{}][deliveryId|{}]"
        log.info(logMsg, objetoId, obligacionId, deliveryId)
      }
    case other =>
      val logMsg = "[AggregateObjeto|{}][WrongMsg|{}]"
      log.error(logMsg, objetoId, other.toString)
  }

  override def receiveRecover: Receive = {
    case evt: Event =>
      log.info(s"replay event: $evt")
      state += evt
    case SnapshotOffer(_, snapshot: StateObjeto) =>
      state = snapshot
  }
}

object AggregateObjeto {
  val typeName = "AggregateObjeto"

  def props(): Props = Props[AggregateObjeto]

  // Protocolo de AggregateObjeto
  sealed trait Command extends Product with Serializable {
    def objetoId: String
    def deliveryId: Long
  }
  final case class UpdateObligacion(
                               objetoId: String,
                               deliveryId: Long,
                               obligacionId: String,
                               obligacion: Double) extends Command

  sealed trait Response extends Product with Serializable {
    def deliveryId: Long
  }
  final case class UpdateSuccess(deliveryId: Long) extends Response

  sealed trait Event extends Product with Serializable { def name: String }
  final case class ObligacionUpdated(obligacionId: String, obligacion: Double) extends Event {
    def name: String = "ObligacionUpdated"
  }

  // State
  final case class StateObjeto private (
                              saldo: Double,
                              obligaciones: Map[String, Double]
                            ) {
    def +(event: Event): StateObjeto = event match {
      case ObligacionUpdated(obligacionId: String, obligacion: Double) =>
        copy(
          saldo = saldo + obligacion,
          obligaciones = obligaciones + (obligacionId -> obligacion)
        )
    }
  }
  object StateObjeto {
    def init(): StateObjeto = new StateObjeto(0, Map.empty[String, Double])
  }

  // Factory Method for AggregateObjeto
  def start (system: ActorSystem)= ClusterSharding(system).start(
    typeName        = typeName,
    entityProps     = this.props(),
    settings        = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId  = extractShardId(1)
  )

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd : Command => (cmd.objetoId, cmd)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case cmd : Command => (cmd.objetoId.toLong % numberOfShards).toString
  }
}