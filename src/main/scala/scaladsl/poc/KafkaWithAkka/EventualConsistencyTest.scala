package poc

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.{ Config, ConfigFactory }
import defaults.kafka.KafkaProducerDefaults.system
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.joda.time.DateTime
import poc.MainPoc.config
import poc.kafka.KafkaSerializer
import poc.model.objeto.AggregateObjeto

import scala.collection.immutable
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }
import sys.process._
import scala.concurrent.ExecutionContext.Implicits.global

object EventualConsistencyTest extends App {

  case class TimeDelta(a: DateTime, b: DateTime)

  val config = new AppConfig(ConfigFactory.load())
  import config.SOURCE_TOPIC

  def publishTransactions(N: Int) = {
    import defaults.kafka.KafkaProducerDefaults._

    def exampleTransaction: AggregateObjeto.UpdateObligacion =
      AggregateObjeto.UpdateObligacion("1", 1L, AggregateObjeto.Obligacion("1", "666", 100, DateTime.now()))

    val appConfig = new AppConfig(ConfigFactory.load())
    def producerSettings[A]: ProducerSettings[String, A] =
      ProducerSettings(system, new StringSerializer, new KafkaSerializer[A])

    def now = DateTime.now()

    def obligacionN(i: Int) =
      exampleTransaction.copy(
        deliveryId = i.toLong,
        obligacion = AggregateObjeto.Obligacion(
          obligacionId    = ((i % 3) + 1).toString,
          sujetoId        = "666",
          saldoObligacion = 100,
          fechaUltMod     = now.plus(100 + i)
        )
      )

    val range: immutable.Seq[AggregateObjeto.UpdateObligacion] =
      for (i <- 1 to N)
        yield obligacionN(i)

    val produce: Future[Done] =
      Source(range)
        .map(value => {
          //println(s"value $value")
          new ProducerRecord[String, AggregateObjeto.UpdateObligacion](SOURCE_TOPIC, value)
        })
        .runWith(Producer.plainSink(producerSettings))

    val delta = produce.map((a: Done) => {
      val start = DateTime.now
      //val end = andThen(N)

      TimeDelta(start, start)
    })

    //produce.andThen  {case _ => system.terminate()}

    delta
  }

  def deploy = Seq(
    "sbt 'docker:stage'",
    "sbt 'docker:publishLocal'",
    "docker-compose up -d",
    "sleep 60"
  ).map(_.!)

  def andThen(N: Int) = {

    def recursion(acc: Int): DateTime = {
      val curl = s"curl http://0.0.0.0:8000/state/objeto/1".!!
      println(curl)
      // success : StateObjeto(2000.0,Map(1 -> Obligacion(1,2,2000.0,2019-08-06T22:43:36.755Z)))
      val result = ("""\d+""".r findAllIn curl).toList.head.toInt
      // 2000
      val foundExpectedResult = result == 2000 * N
      // true || false
      foundExpectedResult match {
        case true => DateTime.now
        case false => recursion(acc + result)
      }
    }
    recursion(0)

  }

  main()
  def main(): Unit = {
    val Ns: Seq[Int] = Seq(
      10000
    )
    val timeDeltas: Seq[Future[TimeDelta]] = Ns.map(publishTransactions(_))

    val result = for {
      deltas <- Future.sequence(timeDeltas)
      _ <- system.terminate()
    } yield for {
      delta <- deltas
    } yield delta

    result onComplete {
      case Success(s) => println(s"success $s")
      case Failure(e) => println(s"fuck $e")
    }
  }
}
