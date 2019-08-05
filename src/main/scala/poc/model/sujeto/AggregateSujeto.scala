package poc.model.sujeto

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.joda.time.DateTime
import poc.model.ddd._

class AggregateSujeto extends PersistentActor with ActorLogging {
  import AggregateSujeto._

  private val sujetoId = self.path.name
  override def persistenceId: String = typeName  + "-" + sujetoId

  private var state: StateSujeto = StateSujeto.init()

  override def receiveCommand: Receive = {
    case UpdateObjeto(_, deliveryId, objeto)
      if state.objetos.exists { case (obId, ob) =>
        obId.equals(objeto.objetoId) &&
          ob.fechaUltMod.isAfter(objeto.fechaUltMod)
      } =>
      // respond success
      val response = UpdateSuccess(deliveryId)
      sender() ! response
      val logMsg = "[{}][ObjetoUpdated|{}][deliveryId|{}]"
      log.info(logMsg, persistenceId, state.objetos(objeto.objetoId), deliveryId)

    case UpdateObjeto(_, deliveryId, objeto) =>
      val evt = ObjetoUpdated(objeto)
      persist(evt) { e =>
        state += e
        // respond success
        val response = UpdateSuccess(deliveryId)
        sender() ! response
        val logMsg = "[{}][ObjetoUpdated|{}][deliveryId|{}]"
        log.info(logMsg, persistenceId, objeto, deliveryId)
      }

    case AggregateSujeto.GetState(_) =>
      val replyTo = sender()
      replyTo ! state
      val logMsg = "[{}][GetState|{}]"
      log.error(logMsg, persistenceId, state.toString)

    case other =>
      val logMsg = "[{}][WrongMsg|{}]"
      log.error(logMsg, persistenceId, other.toString)
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
                           objeto: Objeto) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(deliveryId: Long) extends Response

  final case class ObjetoUpdated(objeto: Objeto) extends Event {
    def name: String = "ObjetoUpdated"
  }

  // State
  final case class Objeto(objetoId: String,
                          saldoObjeto: Double,
                          fechaUltMod: DateTime)

  final case class StateSujeto private (
                              saldo: Double,
                              objetos: Map[String, Objeto]
                            ) {
    def +(event: Event): StateSujeto = event match {
      case ObjetoUpdated(objeto: Objeto) =>
        copy(
          saldo = saldo + objeto.saldoObjeto,
          objetos = objetos + (objeto.objetoId -> objeto)
        )
    }
  }
  object StateSujeto {
    def init(): StateSujeto = new StateSujeto(0, Map.empty[String, Objeto])
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