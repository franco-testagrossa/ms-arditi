package HowToLoadAMusket.service.Messaging

import akka.Done
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future
import scala.util.{Failure, Success}

object KafkaProducer extends App {

  val R = Console.RED
  val B = Console.BLUE
  val G = Console.GREEN
  val Y = Console.YELLOW
  val W = Console.WHITE
  val RT = Console.RESET
  val BB = Console.BLACK
  val RB = Console.RED_B
  println(
    s"""
      |--------$BB$RB Hello, South Regiment.$RT
      |
      |        I will instruct you to fire two volleys.
      |
      |        To do this I will iterate two cycles of the necessary orders:
      |
      |                 - Prepare!
      |                 - Take aim!
      |                 - Fire!
      |
      |        My orders will be sent by a enterprise bus made by $BB${Console.BLUE_B}LinkedIn$RT.
      |        Understood?
      |  $W * No musketmen is hearing anything because he is talking to the console $RT
      |  $W * and they only listen to Kafka *$RT
      |  $Y --musketmen_A : "is he talking to the console again? I can't hear anything he says.. " $RT
      |  $G --musketmen_B : "just nod your head." $RT
      |  $B --musketmen_C :  "he is doing it again, isn't he? Talking to the console." $RT
      |  $Y --musketmen_A : "... this has happened before?? " $RT
      |  $B --musketmen_C :  "all the time. However, I think we are taking to much codebase space by talking. Better to shut up." $RT
      |  $G --musketmen_B : "we are not going to last one single commit if we continue talking." $RT
      |  $W * Nods increase in number. Consensus achieved among the ranks. *$RT
      |
      |
      |        Understood, men? Okay, let's start the volley exercise.
    """.stripMargin
  )

  Thread.sleep(10000)

  import common.kafka.KafkaProducerActorLead._
  val messages = Array(
    "Prepare!",
    "Take aim!",
    "Fire!"
  )
  val messagesColors = Array(
    Console.GREEN_B + Console.BLACK,
    Console.YELLOW_B + Console.BLACK,
    Console.RED_B + Console.BLACK,
  )

  val topic = "test"
  case class Message(s: String, i: Int, topic: String)

  def message(i: Int) = messages(i % messages.length)
  def color(i: Int) = messagesColors(i % messagesColors.length)

  def italic(s: String) = s"\u001B[3m $s $RB"

  def beautifulMessage(implicit i: Message) =  s" ${color(i.i)}$W ${i.s} $RT "
  def beautifulTopic(implicit i: Message) =  s" ${Console.BLUE_B + Console.BLACK}$W ${i.topic} $RT "
  def beautiful(implicit i: Message) =
    s"Sending $beautifulMessage to the $beautifulTopic topic! "


  val produce: Future[Done] =
    Source(0 to 5)
      .map(value => {
        implicit val HANDS_OFF_THE_KEYBOARD = Message(s = message(value), i = value, topic)
        println("")
        println(beautiful)
        println("")
        new ProducerRecord[String, String](topic, message(value))
      })
      .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_) =>
      println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}
