package eggs.infrastructure

import eggs.domain.{ Cook, DomainHelpers }
import org.scalatest.FreeSpec
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InMemoryCookRepositoryTest extends FreeSpec with ScalaFutures with DomainHelpers {

  class TestContext {
    val repo = new InMemoryCookRepository
  }

  "add" - {
    "should find a cook in the repo" in new TestContext {

      val futureResult: Future[Cook] = for {
        cook <- repo.findByName("Fulano")
      } yield cook

      whenReady(futureResult) { result: Cook =>
        assert(result.name != "Cosmefulanito"
          && result.name == "Fulano")
      }
    }
  }

}