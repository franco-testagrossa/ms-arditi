

package eggs.infrastructure

import eggs.domain.Egg.RawEgg
import eggs.domain.EggRepository

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class InMemoryEggRepository(implicit ec: ExecutionContext) extends EggRepository {
  private val eggs = mutable.Queue[RawEgg.type]() // MUTATION!!! NOOO MY EYEEEEES

  override def findAndRemove(): Future[Option[RawEgg.type]] = Future {
    eggs.synchronized {
      Try(eggs.dequeue()).toOption
    }
  }

  override def add(egg: RawEgg.type): Future[Unit] = Future {
    eggs.synchronized {
      eggs.enqueue(egg)
    }
  }
}


