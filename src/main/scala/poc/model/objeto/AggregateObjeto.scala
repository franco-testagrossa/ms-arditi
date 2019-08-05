package poc.model.objeto

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.{PersistentActor, SnapshotOffer}
import org.joda.time.DateTime
import poc.model.ddd._
class AggregateObjeto extends PersistentActor with ActorLogging {
  import AggregateObjeto._

  private val objetoId = self.path.name
  override def persistenceId: String = typeName  + "-" + objetoId

  private var state: StateObjeto = StateObjeto.init()

  override def receiveCommand: Receive = {
    // received old command
    case UpdateObligacion(aggregateRoot, deliveryId, obligacion)
      if state.obligaciones.exists { case (obId, ob) =>
        obId.equals(obligacion.obligacionId) &&
          ob.fechaUltMod.isAfter(obligacion.fechaUltMod)
      } =>
      // respond success
      val existingObligacion = state.obligaciones(obligacion.obligacionId)
      val response = UpdateSuccess(aggregateRoot, deliveryId, existingObligacion)
      sender() ! response
      val logMsg = "[{}][ObligacionUpdated|{}][deliveryId|{}][Old]"
      log.info(logMsg, persistenceId, existingObligacion, deliveryId)

    // update existing obligacion
    case UpdateObligacion(aggregateRoot, deliveryId, obligacion)
      if state.obligaciones.exists { case (obId, ob) =>
        obId.equals(obligacion.obligacionId) &&
          ob.fechaUltMod.isBefore(obligacion.fechaUltMod)
      } =>
      val oldObligacion = state.obligaciones(obligacion.obligacionId)
      val newObligacion =
        oldObligacion.copy(
          saldoObligacion = oldObligacion.saldoObligacion + obligacion.saldoObligacion,
          fechaUltMod = obligacion.fechaUltMod
      )
      val evt = ObligacionUpdated(obligacion)
      persist(evt) { e =>
        state += e
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, obligacion)
        sender() ! response
        val logMsg = "[{}][ObligacionUpdated|{}][deliveryId|{}][Existing]"
        log.info(logMsg, persistenceId, newObligacion, deliveryId)
      }

    // update not existing obligacion
    case UpdateObligacion(aggregateRoot, deliveryId, obligacion) =>
      val evt = ObligacionUpdated(obligacion)
      persist(evt) { e =>
        state += e
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, obligacion)
        sender() ! response
        val logMsg = "[{}][ObligacionUpdated|{}][deliveryId|{}][New]"
        log.info(logMsg, persistenceId, obligacion, deliveryId)
      }

    case AggregateObjeto.GetState(_) =>
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
                               obligacion: Obligacion) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(aggregateRoot: String, deliveryId: Long, obligacion: Obligacion) extends Response


  final case class ObligacionUpdated(obligacion: Obligacion) extends Event {
    def name: String = "ObligacionUpdated"
  }

  // State
  final case class Obligacion(obligacionId: String,
                              sujetoId: String,
                              saldoObligacion: Double,
                              fechaUltMod: DateTime)

  final case class StateObjeto private (
                              saldo: Double,
                              obligaciones: Map[String, Obligacion]
                            ) {
    def +(event: Event): StateObjeto = event match {
      case ObligacionUpdated(obligacion: Obligacion) =>
        copy(
          saldo = saldo + obligacion.saldoObligacion,
          obligaciones = obligaciones + (obligacion.obligacionId -> obligacion)
        )
    }
  }
  object StateObjeto {
    def init(): StateObjeto = new StateObjeto(0, Map.empty[String, Obligacion])
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