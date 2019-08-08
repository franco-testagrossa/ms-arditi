package poc

import org.scalatest.WordSpec

class BasicSpec extends WordSpec {

  "test" should {
    "validate" in {
      val r = for {
        did <- 1 to 10 //
        oblid <- 1 to 3
        objid <- 1 to 2
        sid <- 1 to 2
      } yield (did, oblid, objid, sid)



      println(r.map(_._1).distinct.size)
      println(r.map(_._2).distinct.size)
      println(r.map(_._3).distinct.size)
      println(r.map(_._4).distinct.size)
    }
  }

}
