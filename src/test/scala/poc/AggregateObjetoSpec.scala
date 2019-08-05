package poc

import akka.actor.Kill
import org.joda.time.DateTime
import poc.model.objeto.AggregateObjeto
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.concurrent.duration._

class AggregateObjetoSpec extends ClusterArditiSpec {


  "The AggregateObjeto" should {
    "should update obligacion with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val (obligacionId, obligacionSaldo, sujetoId, objetoId) = ("1", 200.50, "999", "3")
      val objeto = supervisor.create(AggregateObjeto.props(), "AggregateObjeto-1")

      objeto ! AggregateObjeto.GetState("1")
      objeto ! AggregateObjeto.UpdateObligacion("1", 1L, AggregateObjeto.Obligacion(
        obligacionId, sujetoId, obligacionSaldo, DateTime.now()
      ))
      // TODO: Test receive recover
      // objeto ! Kill
      // Thread.sleep(500)
      objeto ! AggregateObjeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObjeto.UpdateSuccess(aggregateRoot, 1L, obligacion) => true
        }
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == obligacionSaldo && obligaciones.contains(obligacionId) => true
        }
      }

      supervisor.stop()
      Thread.sleep(200)
    }

    "should update obligacion with sharding" in {
      val (obligacionId, obligacionSaldo, sujetoId, objetoId) = ("2", 200.50, "1000", "4")
      val objeto = AggregateObjeto.start

      objeto ! AggregateObjeto.GetState("2")
      objeto ! AggregateObjeto.UpdateObligacion("2", 1L, AggregateObjeto.Obligacion(
        obligacionId, sujetoId, obligacionSaldo, DateTime.now()
      ))
      objeto ! AggregateObjeto.GetState("2")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObjeto.UpdateSuccess(aggregateRoot, 1L, obligacion) => true
        }
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == obligacionSaldo && obligaciones.contains(obligacionId) => true
        }
      }
    }
  }
}
