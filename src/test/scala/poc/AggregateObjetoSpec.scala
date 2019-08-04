package poc

import akka.actor.Kill
import poc.objeto.AggregateObjeto
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.concurrent.duration._

class AggregateObjetoSpec extends ClusterArditiSpec {

  "The AggregateObjeto" should {
    "should update obligacion with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val (obligacionId, obligacionSaldo) = ("1", 200.50)
      val objeto = supervisor.create(AggregateObjeto.props(), "AggregateObjeto-1")

      objeto ! AggregateObjeto.GetState("1")
      objeto ! AggregateObjeto.UpdateObligacion("1", 1L, obligacionId, obligacionSaldo)
      objeto ! Kill
      Thread.sleep(200)
      objeto ! AggregateObjeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObjeto.UpdateSuccess(1L) => true
        }
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == obligacionSaldo && obligaciones.contains(obligacionId) => true
        }
      }
    }

    "should update obligacion with sharding" in {
      val (obligacionId, obligacionSaldo) = ("1", 200.50)
      val objeto = AggregateObjeto.start

      objeto ! AggregateObjeto.GetState("1")
      objeto ! AggregateObjeto.UpdateObligacion("1", 1L, obligacionId, obligacionSaldo)
//      objeto ! Kill
//      Thread.sleep(200)
      objeto ! AggregateObjeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateObjeto.UpdateSuccess(1L) => true
        }
        expectMsgPF() {
          case AggregateObjeto.StateObjeto(saldo, obligaciones)
            if saldo == obligacionSaldo && obligaciones.contains(obligacionId) => true
        }
      }
    }
  }
}
