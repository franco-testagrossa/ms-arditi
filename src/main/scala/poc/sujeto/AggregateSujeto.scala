package poc.sujeto

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}
import poc.ddd._

class AggregateSujeto extends PersistentActor with ActorLogging {
  import AggregateSujeto._

  private val sujetoId = self.path.name
  override def persistenceId: String = typeName  + "-" + sujetoId

  private var state: StateSujeto = StateSujeto.init()
  private var lastDeliveredId: Long = 0L // handling ordering

  override def receiveCommand: Receive = {
    case UpdateObjeto(_, deliveryId, objetoId, objeto)
      if lastDeliveredId > deliveryId => // drop the message (ordering)
    case UpdateObjeto(_, deliveryId, objetoId, objeto) =>
      val evt = ObjetoUpdated(objetoId, objeto)
      persist(evt) { e =>
        state += e
        lastDeliveredId = lastDeliveredId max deliveryId
        // respond success
        val response = UpdateSuccess(deliveryId)
        sender() ! response
        val logMsg = "[AggregateSujeto|{}][ObjetoUpdated|{}][deliveryId|{}]"
        log.info(logMsg, sujetoId, objetoId, deliveryId)
      }
    case AggregateSujeto.GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[AggregateSujeto|{}][GetState|{}]"
      log.error(logMsg, sujetoId, state.toString)
    case other =>
      val logMsg = "[AggregateSujeto|{}][WrongMsg|{}]"
      log.error(logMsg, sujetoId, other.toString)
  }

  override def receiveRecover: Receive = {
    case evt: Event =>
      log.info(s"replay event: $evt")
      state += evt
    case SnapshotOffer(_, snapshot: StateSujeto) =>
      state = snapshot
  }
}

object AggregateSujeto {

  val typeName = "AggregateSujeto"

  def props(): Props = Props[AggregateSujeto]

  final case class UpdateObjeto(
                                 aggregateRoot: String,
                           deliveryId: Long,
                           objetoId: String,
                           objeto: Double) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(deliveryId: Long) extends Response

  final case class ObjetoUpdated(objetoId: String, objeto: Double) extends Event {
    def name: String = "ObjetoUpdated"
  }

  // State
  final case class StateSujeto private (
                              saldo: Double,
                              objetos: Map[String, Double]
                            ) {
    def +(event: Event): StateSujeto = event match {
      case ObjetoUpdated(objetoId: String, objeto: Double) =>
        copy(
          saldo = saldo + objeto,
          objetos = objetos + (objetoId -> objeto)
        )
    }
  }
  object StateSujeto {
    def init(): StateSujeto = new StateSujeto(0, Map.empty[String, Double])
  }

  // Factory Method for AggregateSujeto
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