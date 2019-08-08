package poc

import org.joda.time.DateTime
import org.scalatest.WordSpec
import poc.model.objeto.AggregateObjeto
import poc.model.objeto.AggregateObjeto.{Obligacion, ObligacionUpdated}

class StateObjetoSpec extends WordSpec {

  "StateObjeto" should {
    "calculate sujeto report" in {
      def printState(s: AggregateObjeto.StateObjeto, obligacion: Obligacion): Unit = {
        println(s.sujetoReport(obligacion))
        println(s.saldo)
      }
      val obligacion = Obligacion("1", "1", 100.0, DateTime.now)
      val s0 = AggregateObjeto.StateObjeto.init()
      printState(s0, obligacion)
      val s1 = s0 + ObligacionUpdated(obligacion) // u1
      printState(s1, obligacion)
      val s2 = s1 + ObligacionUpdated(obligacion) // u2
      printState(s2, obligacion)
      val s3 = s2 + ObligacionUpdated(obligacion.copy(sujetoId = "2")) // u3
      printState(s3, obligacion)
      val s4 = s3 + ObligacionUpdated(obligacion.copy(sujetoId = "2")) // u3
      printState(s4, obligacion)
      val s5 = s4 + ObligacionUpdated(obligacion.copy(sujetoId = "3")) // u3
      printState(s5, obligacion)

    }
  }
}
