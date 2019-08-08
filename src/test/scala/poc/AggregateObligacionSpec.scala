package poc

import akka.actor.Kill
import akka.util.Timeout
import org.joda.time.DateTime
import poc.model.objeto.AggregateObjeto.Obligacion
import poc.model.obligacion.AggregateObligacion
import poc.model.obligacion.AggregateObligacion
import poc.model.obligacion.AggregateObligacion.Movimiento
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class AggregateObligacionSpec extends ClusterArditiSpec {

  "The AggregateObligacion" should {

    val expectedSaldo = 200

    "should update sujeto with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val obligacion = supervisor.create(AggregateObligacion.props, "AggregateObligacion-1")

      obligacion ! AggregateObligacion.GetState("1")
      obligacion ! AggregateObligacion.UpdateMovimiento(
        aggregateRoot = "1",
        deliveryId = 1L,
        movimiento = Movimiento(
          evoId = "1",
          objetoId = "1",
          sujetoId = "1",
          importe = expectedSaldo,
          tipoMovimiento= 'C',
          fechaUltMod = DateTime.now
        ))
      
      obligacion ! Kill
      Thread.sleep(200)

      obligacion ! AggregateObligacion.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObligacion.StateObligacion(saldo, movimientos)
            if saldo == 0 && movimientos.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObligacion.UpdateSuccess(_, 1L, _) => true
        }
        expectMsgPF() {
          case AggregateObligacion.StateObligacion(saldo, movimientos)
            if saldo == expectedSaldo => true
        }
      }

      supervisor.stop()
      Thread.sleep(200)
    }

    "should update obligacion with sharding" in {
      val obligacion = AggregateObligacion.start

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).map(
        _ => obligacion ? AggregateObligacion.UpdateMovimiento(
          aggregateRoot = "1",
          deliveryId = N.toLong,
          movimiento = Movimiento(
            evoId = "1",
            objetoId = "1",
            sujetoId = "1",
            importe = expectedSaldo,
            tipoMovimiento= 'C',
            fechaUltMod = DateTime.now
          ))
        )
      )

      Await.result(transactions, 10 second)

      val state = (obligacion ? AggregateObligacion.GetState("1")).mapTo[AggregateObligacion.StateObligacion]
      state.foreach { a =>
        println(s"Expected saldo is: ${expectedSaldo * N}, and saldo is ${a.saldo}")
        assert(a.saldo == expectedSaldo * N)
      }
    }
  }
}
