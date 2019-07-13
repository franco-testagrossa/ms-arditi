

package eggs.domain

import scala.concurrent.Future

trait CookRepository {
  def findOne(): Future[Cook] = ???
  def findByName(name: String): Future[Cook] = ???
}

