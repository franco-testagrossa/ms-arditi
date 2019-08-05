package poc

import akka.actor.Kill
import org.joda.time.DateTime
import poc.model.sujeto.AggregateSujeto
import poc.model.sujeto.AggregateSujeto.Objeto
import sagas.utils.{ClusterArditiSpec, RestartActorSupervisorFactory}

import scala.concurrent.duration._

class AggregateSujetoSpec extends ClusterArditiSpec {

  "The AggregateSujeto" should {
    "should update objeto with supervisor" in {
      val supervisor = new RestartActorSupervisorFactory

      val (objetoId, objetoSaldo) = ("1", 200.50)
      val objeto = supervisor.create(AggregateSujeto.props(), "AggregateSujeto-1")

      objeto ! AggregateSujeto.GetState("1")
      objeto ! AggregateSujeto.UpdateObjeto("1", 1L, Objeto(objetoId, objetoSaldo, DateTime.now()))
      // TODO: Test receive recover
      // objeto ! Kill
      // Thread.sleep(200)
      objeto ! AggregateSujeto.GetState("1")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == 0 && objetos.isEmpty => true
        }
        expectMsgPF() {
          case AggregateSujeto.UpdateSuccess(1L) => true
        }
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == objetoSaldo && objetos.contains(objetoId) => true
        }
      }

      supervisor.stop()
      Thread.sleep(200)
    }

    "should update objeto with sharding" in {
      val (objetoId, objetoSaldo) = ("2", 200.50)
      val objeto = AggregateSujeto.start

      objeto ! AggregateSujeto.GetState("2")
      objeto ! AggregateSujeto.UpdateObjeto("2", 1L, Objeto(objetoId, objetoSaldo, DateTime.now()))
//      objeto ! Kill
//      Thread.sleep(200)
      objeto ! AggregateSujeto.GetState("2")

      within(3 seconds) {
        expectMsgPF() {
          case AggregateSujeto.StateSujeto(saldo, objetos)
            if saldo == 0 && objetos.isEmpty => true
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
