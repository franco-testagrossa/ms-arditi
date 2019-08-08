package poc.model.sujeto

import akka.actor.{ ActorLogging, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.persistence.{ PersistentActor, SnapshotOffer }
import common.cqrs.ShardedEntity
import org.joda.time.DateTime
import poc.model.ddd._
class AggregateSujeto extends PersistentActor with ActorLogging {
  import AggregateSujeto._

  private val objetoId = self.path.name
  override def persistenceId: String = typeName + "-" + objetoId

  private var state: StateSujeto = StateSujeto.init()

  override def receiveCommand: Receive = {

    case UpdateObjeto(aggregateRoot, deliveryId, objeto) =>
      val evt = ObjetoUpdated(objeto)
      persist(evt) { e =>

        //val objetosToSaldos: Map[String, Double] = state.objetos.map(o => (o._1 -> o._2.saldoObjeto))
        //val b = Console.WHITE
        //println(s"$b Sujeto ids:${if (state.objetos.size > 1) state.objetos.map(p => p._2.sujetoId)}: ${objetosToSaldos} \t\t\t\t\t\t\t - before: ${state.saldo}: " + s" - after: ${(state.+(e).saldo)}")
        state += e
        // respond success
        val response = UpdateSuccess(aggregateRoot, deliveryId, objeto)
        sender() ! response
        val logMsg = "[{}][ObjetoUpdated|{}][deliveryId|{}][New]"
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

object AggregateSujeto extends ShardedEntity {

  val typeName = "AggregateSujeto"

  val props: Props = Props[AggregateSujeto]

  final case class UpdateObjeto(
      aggregateRoot: String,
      deliveryId:    Long,
      objeto:        Objeto

  ) extends Command

  final case class GetState(aggregateRoot: String) extends Query

  final case class UpdateSuccess(aggregateRoot: String, deliveryId: Long, objeto: Objeto) extends Response

  final case class ObjetoUpdated(objeto: Objeto) extends Event {
    def name: String = "ObjetoUpdated"
  }

  // State
  final case class Objeto(
      objetoId:    String,
      sujetoId:    String,
      saldoObjeto: Double,
      fechaUltMod: DateTime
  )

  final case class StateSujeto private (
      saldo:   Double,
      objetos: Map[String, Objeto]
  ) {
    def +(event: Event): StateSujeto = event match {
      case ObjetoUpdated(objeto: Objeto) =>
        copy(
          saldo   = calculateSaldo(objeto),
          objetos = updateObjetos(objeto)
        )
    }

    def calculateSaldo(o: Objeto): Double = saldo + o.saldoObjeto // is a delta with +- sign
    def updateObjetos(o: Objeto): Map[String, Objeto] = {
      val saldoDelta = o.saldoObjeto
      objetos.get(o.objetoId).map { ob =>
        val oldSaldo = ob.saldoObjeto
        val newObjeto = o.copy(saldoObjeto = saldoDelta + oldSaldo)
        newObjeto
      } match {
        case Some(newObjeto) => objetos + (newObjeto.objetoId -> newObjeto)
        case None => objetos + (o.objetoId -> o)
      }
    }
  }
  object StateSujeto {
    def init(): StateSujeto = new StateSujeto(0, Map.empty[String, Objeto])
  }
}
