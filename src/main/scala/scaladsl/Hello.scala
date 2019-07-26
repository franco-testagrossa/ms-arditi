import PAImpl.Run
import akka.actor.{ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.PersistentActor

import scala.concurrent.Future
import scala.util.{Failure, Success}


object PAImplClient extends App {

  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._

  val as = ActorSystem("ClusterArditi")
  val paImpl: ActorRef = PAImpl.start(as) // Factory

  implicit val ec = as.dispatcher
  implicit val timeout = Timeout(3 seconds)
  // https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html
  val response = for {
    a <- (paImpl ? Run("1")).mapTo[Option[String]]
    b <- (paImpl ? Run("0")).mapTo[Option[String]]
    c <- (paImpl ? Run("2")).mapTo[Option[String]]
  } yield (a, b, c)

  response.onComplete {
    case Failure(exception) =>
      println(s"response exception $exception")
    case Success(value) =>
      println(s"response value $value")
  }

  Thread.sleep(2000)
  //paImpl ! PoisonPill
  Thread.sleep(2000)
  // val rehidratated: ActorRef = PAImpl.start(as) // Factory
  (paImpl ? PAImpl.Run("1")).mapTo[Option[String]].onComplete {
    case Failure(exception) =>
      println(s"rehidratated exception $exception")
    case Success(value) =>
      println(s"rehidratated value $value")
  }

}
/*
Nos queremos abstraer de:
- no escribir boiler plate dentro del companion object
- no escribir inicializacion dentro del PersistentActor
  trait PersistentEntity extends PersistentActor {
      ...
  }
- queremos atar el protocolo de CMD (ADT) con el State y sus Eventos =>
  VAMOS A SER PUROS PARA TESTEAR EL State que resulta ser un Aggregate, separado de la logica que va a ejecutar el side effect del Actor
    + CMD <-1 a 1-> EVT
    + CMD <-1 a 1-> Response
    + STATE - 1 a *-> EVT
    + STATE - 1 a *-> Response


    *Potenciaal solucion*
    // Ya tenemos el ADT -> Free.lift(ADT) => Service
    // El estado que mantiene ese modulo ya lo tenes State(Aggregate)
    // Tenemos la garantia que podemos MUTAR State sin preocuparnos por concurrencia


- no escribir logica sobre las garantias que provee el actor a sus clientes
  + que pueda soportar deduplicacion de commandos (emitiendo de forma idempotente la misma respuesta a todos [Cache])
  + que pueda soportar la garantia de Delivery (trait AtLeastOnce semantics)


  Emisor ---- Envia Commando ----> Receptor
         <---- Respuesta    -------- Si lo manejo antes responde
                                   - Si no lo manejo antes lo procesa
                                       hablas con el protocolo y obtenes respuesta del mismo en forma de PersistentEffect

                                       (El procesamiento del comando esta embebido dentro del State)
                                        y es una logica pura y testeable fuera del contexto del Actor

   PersistentEffect
      Describe que effecto(accion) debiera de ejecutar el persitent actor ante dicha respuesta
      - como persiste ?
      - como responde ?
      - como cambia el estado ?
 */



// type Receive = PartialFunction[Any, Unit]
class PAImpl extends PersistentActor with ActorLogging {
  import PAImpl._

  override def persistenceId: String = typeName  + "-" + self.path.name


  var state: String = "" // State.init
  override def receiveCommand: Receive = {
    case cmd: PAImpl.Command =>
      /*
      val machine => interpreter.interpret(state)
      val newMachine: PersistentEffect[State] = machine.run(cmd)
      newMachine.flatMap { newSt =>
        state += evt.id // state is updated
        sender() ! Some(state) // a Response (Some) is sent back to client
      }
       */

      log.info("cmd {}", cmd)
      if(cmd.id > "0") {
        persist(Runned(cmd.id)) { evt =>
          state += evt.id // state is updated
          sender() ! Some(state) // a Response (Some) is sent back to client
        }
      } else {
        sender() ! Some(state) // a Response (Some) is sent back to client
      }
  }

  override def receiveRecover: Receive = {
    case evt: PAImpl.Event =>
      state += evt.id
  }
}

object PAImpl {
  val typeName = "PAImpl"

  def props(): Props = Props[PAImpl]

  // Protocolo de PAImpl
  trait Command { def id: String }
  case class Run(id: String) extends Command

  trait Event { def id: String }
  case class Runned(id: String) extends Event

  // Factory Method for PAImpl
  def start (system: ActorSystem)= ClusterSharding(system).start(
    typeName        = typeName,
    entityProps     = this.props(),
    settings        = ClusterShardingSettings(system),
    extractEntityId = extractEntityId,
    extractShardId  = extractShardId(1)
  )

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd : Command => (cmd.id, cmd)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case cmd : Command => (cmd.id.toLong % numberOfShards).toString
  }
}

