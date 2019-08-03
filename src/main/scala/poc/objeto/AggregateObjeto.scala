package poc.objeto

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}

import poc.transaction.ActorRefFlowStage.{StreamElementIn, StreamElementOut}
import poc.ddd._
class AggregateObjeto extends PersistentActor with ActorLogging {
  import AggregateObjeto._

  private val objetoId = self.path.name
  override def persistenceId: String = typeName  + "-" + objetoId

  private var state: StateObjeto = StateObjeto.init()
  private var lastDeliveredId: Long = 0L // handling ordering

  override def receiveCommand: Receive = {
    case StreamElementIn(UpdateObligacion(_, deliveryId, obligacionId, obligacion))
      if lastDeliveredId > deliveryId => // drop the message (ordering)
    case StreamElementIn(UpdateObligacion(_, deliveryId, obligacionId, obligacion)) =>
      val evt = ObligacionUpdated(obligacionId, obligacion)
      persist(evt) { e =>
        state += e
        lastDeliveredId = lastDeliveredId max deliveryId
        // respond success
        val response = UpdateSuccess(deliveryId)
        sender() ! StreamElementOut(response)
        val logMsg = "[AggregateObjeto|{}][ObligacionUpdated|{}][deliveryId|{}]"
        log.info(logMsg, objetoId, obligacionId, deliveryId)
      }
    case AggregateObjeto.GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[AggregateObjeto|{}][GetState|{}]"
      log.error(logMsg, objetoId, state.toString)

    // case str:String => sender() ! "HELLO SHIT" + str

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

  final case class UpdateObligacion(
                               aggregateRoot: String,
                               deliveryId: Long,
                               obligacionId: String,
                               obligacion: Double) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(deliveryId: Long) extends Response


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
  def start (implicit system: ActorSystem)= ClusterSharding(system).start(
    typeName        = typeName,
    entityProps     = this.props(),
    settings        = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId  = extractShardId(1)
  )

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case qry : Query => (qry.aggregateRoot, qry)
    case cmd : Command => (cmd.aggregateRoot, cmd)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case qry : Query => (qry.aggregateRoot.toLong % numberOfShards).toString
    case cmd : Command => (cmd.aggregateRoot.toLong % numberOfShards).toString
  }
}