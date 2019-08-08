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

class AggregateObjetoSpec extends ClusterArditiSpec {

  "The AggregateObjeto" should {
    val expectedSaldo = 200
    "should update sujeto with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val objeto = supervisor.create(AggregateObjeto.props, "AggregateObjeto-1")

      objeto ! AggregateObjeto.GetState("1")
      objeto ! AggregateObjeto.UpdateObligacion(
        aggregateRoot = "1",
        deliveryId = 1L,
        obligacion = Obligacion(
          obligacionId = "1",
          sujetoId = "1",
          saldoObligacion = expectedSaldo,
          fechaUltMod = DateTime.now
        ))

      objeto ! Kill
      Thread.sleep(200)

      objeto ! AggregateObjeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObjeto.UpdateSuccess(_, 1L, _) => true
        }
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == expectedSaldo => true
        }
      }

      supervisor.stop()
      Thread.sleep(200)
    }

    "should update objeto with sharding" in {
      val objeto = AggregateObjeto.start

      import akka.pattern._
      implicit val timeout: Timeout = Timeout(10 seconds)

      val N = 10

      val transactions: Future[immutable.IndexedSeq[Any]] = Future.sequence(
        (1 to N).flatMap( deliveryId =>
          (1 to 2).map(
            obligacionId =>
              objeto ? AggregateObjeto.UpdateObligacion(
              aggregateRoot = "1",
              deliveryId = deliveryId,
              obligacion = Obligacion(
                obligacionId = obligacionId.toString,
                sujetoId= "1",
                saldoObligacion= expectedSaldo,
                fechaUltMod=     DateTime.now
              ))
      )))

      Await.result(transactions, 10 second)

      val state = (objeto ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto]
      state.foreach { a =>
        println(s"Expected saldo is: ${expectedSaldo * N * 2}, and saldo is ${a.saldo}")
        assert(a.saldo == expectedSaldo * N * 2)
        assert(
          a.obligaciones.collect {
            case (_, o@Obligacion(_, "1", saldo, _)) if saldo == expectedSaldo * N => o
          }.size == 2
        )
      }
    }
  }
}
