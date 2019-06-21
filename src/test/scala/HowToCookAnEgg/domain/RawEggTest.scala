package eggs.domain

import org.scalatest.FreeSpec
/*

HOW TO RUN
testOnly *RawEggTest
 */
class RawEggTest extends FreeSpec with DomainHelpers {
  "startCooking" - {
    "should return a partially cooked egg" in {
      val style = createEggStyle()
      val rawEgg = createRawEgg()

      val partiallyCookedEgg = rawEgg.startCooking(style)

      assert(partiallyCookedEgg.style === style)
    }
  }
}
