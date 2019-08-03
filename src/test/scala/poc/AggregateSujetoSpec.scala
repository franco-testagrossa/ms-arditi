package poc

import akka.actor.Kill
import poc.sujeto.AggregateSujeto
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.concurrent.duration._

class AggregateSujetoSpec extends ClusterArditiSpec {

  "The AggregateSujeto" should {
    "should update objeto with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val (objetoId, objetoSaldo) = ("1", 200.50)
      val objeto = supervisor.create(AggregateSujeto.props(), "AggregateSujeto-1")

      objeto ! AggregateSujeto.GetState("1")
      objeto ! AggregateSujeto.UpdateObjeto("1", 1L, objetoId, objetoSaldo)
      objeto ! Kill
      Thread.sleep(200)
      objeto ! AggregateSujeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateSujeto.UpdateSuccess(1L) => true
        }
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == objetoSaldo && objetos.contains(objetoId) => true
        }
      }
    }

    "should update objeto with sharding" in {
      val (objetoId, objetoSaldo) = ("1", 200.50)
      val objeto = AggregateSujeto.start(system)

      objeto ! AggregateSujeto.GetState("1")
      objeto ! AggregateSujeto.UpdateObjeto("1", 1L, objetoId, objetoSaldo)
//      objeto ! Kill
//      Thread.sleep(200)
      objeto ! AggregateSujeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, obligaciones)
            if saldo == 0 && obligaciones.isEmpty => true
        }
        expectMsgPF() {
          case AggregateSujeto.UpdateSuccess(1L) => true
        }
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == objetoSaldo && objetos.contains(objetoId) => true
        }
      }
    }
  }
}
