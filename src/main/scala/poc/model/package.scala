package poc

import akka.kafka.ConsumerMessage.TransactionalMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import scalaz.Functor

package object model {
  type TX[A] = TransactionalMessage[String, A]
  object TX {
    // dado un T de CR[A] devuelve un T de CR[B]
    implicit def transactionalMessageFunctor(implicit crFunctor: Functor[CR]) = new Functor[TX] {
      override def map[A, B](fa: TX[A])(f: A => B): TX[B] = {
        val a = fa.record.value()
        val b = f(a)
        val newRecord = crFunctor.map(fa.record)(f)
        TransactionalMessage(newRecord, fa.partitionOffset)
      }
    }
    type CR[A] = ConsumerRecord[String, A]
    implicit val consumerRecordFunctor = new Functor[CR] {
      override def map[A, B](fa: CR[A])(f: A => B): CR[B] = new ConsumerRecord[String, B](
        fa.topic(),
        fa.partition(),
        fa.offset(),
        fa.timestamp(),
        fa.timestampType(),
        null,
        fa.serializedKeySize(),
        fa.serializedValueSize(),
        fa.key(),
        f(fa.value()),
        fa.headers(),
        fa.leaderEpoch()
      )
    }
  }
}
