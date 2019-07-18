package eggs.infrastructure

import eggs.domain.{ Cook, CookRepository }

import scala.concurrent.{ Future }

class InMemoryCookRepository extends CookRepository {
  override def findOne(): Future[Cook] = {
    Future.successful(Cook())
  }
  override def findByName(name: String): Future[Cook] = {
    Future.successful(Cook(name))
  }
}
