package transaction

import akka.Done
import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.{ Consumer, Transactional }
import akka.stream.scaladsl.{ Keep, RestartSource, Sink }
import akka.stream.{ ActorMaterializer, Materializer }
import io.circe
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{ Decoder, Encoder }
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

object Transaction {

  def □[Input, Output: ClassTag](
    SOURCE_TOPIC: String,
    SINK_TOPIC:   String,
    key:          String,
    algorithm:    Input => Future[Seq[Output]]
  )(implicit
    system: ActorSystem,
    consumer: ConsumerSettings[String, String],
    producer: ProducerSettings[String, String],
    decoder:  Decoder[Input],
    encoder:  Encoder[Output]
  ): (Consumer.Control, Future[Seq[Seq[String]]]) = {

    type Msg = ConsumerMessage.TransactionalMessage[String, String]

    implicit val mat: Materializer = ActorMaterializer()(system)
    implicit val ec: ExecutionContext = system.dispatcher

    val begin = Transactional
      .source(consumer, Subscriptions.topics(SOURCE_TOPIC))

    val perform =
      begin.mapAsync(1) {
        msg: ConsumerMessage.TransactionalMessage[String, String] =>

          val input: String = msg.record.value()
          val decoded: Either[circe.Error, Input] = decode[Input](input)(decoder)

          decoded match {
            case Left(error) =>
              val errorMessage = s"Failed to decode $input from topic $SOURCE_TOPIC. Here is the error: $error"
              println(errorMessage)
              Future.failed(new Exception(errorMessage))
            case Right(decoded) => {
              val result: Future[Seq[Output]] = algorithm(decoded)

              type resultType = (Msg, Seq[String], String)
              val xx: Future[resultType] = result.map(_ map {
                _.asJson(encoder).noSpaces
              })
                .map((msg, _, SINK_TOPIC))
                .recover[resultType] {
                  case a: Throwable => (msg, Seq(a.getMessage), "Throwable")
                }
              xx
            }

          }
      }

    //val valuables = perform.collect { case Success(a) => a }

    val commit = perform.map {
      case (msg: Msg, output: Seq[String], sinkTopic: String) =>

        ProducerMessage.multi(
          records     = output.map { o =>
            new ProducerRecord(
              sinkTopic,
              key,
              o
            )
          }.toList,
          passThrough = msg.partitionOffset
        )
    }
      .alsoToMat(Transactional.sink(producer, transactionalId))(Keep.left)
      .collect {
        case a: ProducerMessage.MultiMessage[_, String, _] =>
          a.records.map(b => b.value())

      }
      .toMat(Sink.seq[Seq[String]])(Keep.both)
      .run()

    commit
  }

  def transactionalId: String = java.util.UUID.randomUUID().toString
}

