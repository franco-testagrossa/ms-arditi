package poc

import akka.actor.Kill
import akka.util.Timeout
import org.joda.time.DateTime
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.Obligacion
import poc.model.sujeto.AggregateSujeto
import poc.model.sujeto.AggregateSujeto.Objeto
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class AggregateSujetoSpec extends ClusterArditiSpec {

  "The AggregateSujeto" should {

    val expectedSaldo = 2000.0

    "should be update, be killed, and restarted it with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val sujeto = supervisor.create(AggregateSujeto.props, "AggregateSujeto-1")

      sujeto ! AggregateSujeto.GetState("1")
      sujeto ! AggregateSujeto.UpdateObjeto(
        aggregateRoot = "1",
        deliveryId = 1L,
        objeto = Objeto(
          objetoId = "1",
          sujetoId= "1",
          saldoObjeto= expectedSaldo,
          fechaUltMod=     DateTime.now
        ))

      sujeto ! Kill
      Thread.sleep(200)

      sujeto ! AggregateSujeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == 0 && objetos.isEmpty => true
        }
        expectMsgPF() {
          case AggregateSujeto.UpdateSuccess(_, 1L, _) => true
        }
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == expectedSaldo => true
        }
      }

      supervisor.stop()
    }

    "should update sujeto with sharding" in {
      val objeto = AggregateSujeto.start

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).flatMap {
          deliveryId =>
            (1 to 2).map { objetoId =>
              objeto ? AggregateSujeto.UpdateObjeto(
                aggregateRoot = "1",
                deliveryId = deliveryId.toLong,
                objeto = Objeto(
                  objetoId = objetoId.toString,
                  sujetoId = "1",
                  saldoObjeto = expectedSaldo,
                  fechaUltMod = DateTime.now
                ))
            }

        })

      Await.result(transactions, 10 second)

      val state = (objeto ? AggregateSujeto.GetState("1")).mapTo[AggregateSujeto.StateSujeto]
      state.foreach { a =>
        println(s"Expected saldo is: ${expectedSaldo * N * 2}, and saldo is ${a.saldo}")
        assert(a.saldo == expectedSaldo * N * 2)
        assert(
          a.objetos.collect {
            case (_, o@Objeto(_, "1", saldo, _)) if saldo == expectedSaldo * N * 2 => o
          }.size == 2
        )
      }
    }
  }
}
