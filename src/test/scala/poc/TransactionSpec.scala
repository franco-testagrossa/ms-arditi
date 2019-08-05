/*
 * Copyright (C) 2014 - 2016 Softwaremill <http://softwaremill.com>
 * Copyright (C) 2016 - 2019 Lightbend Inc. <http://www.lightbend.com>
 */

package poc


import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.ConsumerMessage.{PartitionOffset, TransactionalMessage}
import akka.kafka.scaladsl.Consumer.{Control, DrainingControl}
import akka.kafka.scaladsl.{Consumer, Producer, Transactional}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import akka.stream.testkit.scaladsl.TestSink
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.joda.time.DateTime
import poc.kafka.{KafkaDeserializer, KafkaSerializer}
import poc.model.objeto.AggregateObjeto
import poc.model.sujeto.AggregateSujeto

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class TransactionSpec
  extends DocsSpecBase(KafkaPorts.ScalaTransactionsExamples) {

  override def sleepAfterProduce: FiniteDuration = 10.seconds


  val consumerSettings = ConsumerSettings(system,
    new StringDeserializer, new KafkaDeserializer[AggregateObjeto.UpdateObligacion])
    .withBootstrapServers(bootstrapServers)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withGroupId(createGroupId())

  val producerSettings = ProducerSettings(
    system, new StringSerializer, new KafkaSerializer[AggregateObjeto.UpdateObligacion])
    .withBootstrapServers(bootstrapServers)


  "Transactional sink" should "work" in assertAllStagesStopped {
    //val producerSettings = producerDefaults
    val sourceTopic = createTopic(1)
    val sinkTopic = createTopic(2)
    val transactionalId = createTransactionalId()

    import akka.pattern.ask

    def businessFlow[T:ClassTag](actorRef: ActorRef): Flow[T, T, NotUsed] =
    Flow[T].mapAsync(1){ msg =>

      val response = actorRef.ask(msg).mapTo[T]

      response
    }

    val objetoAggregateRef = system.actorOf(AggregateObjeto.props(), "objeto")

    class EchoActor extends Actor with ActorLogging{
      override def receive: Receive = {

        case t: TransactionalMessage[_,_] =>
          log.info("ACTOR RECEIVED {}", t.record.value() )


          import akka.pattern.pipe

          val futureResponseFromObjectAggregate = (objetoAggregateRef ? (t.record.value()))
          val replyTo = sender()


          futureResponseFromObjectAggregate.map{ result =>
              t.copy(record = new ConsumerRecord(
                t.record.topic(),
                t.record.partition(),
                t.record.offset(),
                t.record.key(),
                result
              ))
          }.pipeTo(replyTo)

      }
    }
    val actorRef = system.actorOf(Props(new EchoActor))


    // #transactionalSink
    val control =
      Transactional
        .source(consumerSettings, Subscriptions.topics(sourceTopic))
        .via(businessFlow(actorRef))
        .map { msg: TransactionalMessage[String, AggregateObjeto.UpdateObligacion] =>
          ProducerMessage.single(new ProducerRecord(sinkTopic, msg.record.key, msg.record.value), msg.partitionOffset)
        }
        .toMat(Transactional.sink(producerSettings, transactionalId))(Keep.both)
        .mapMaterializedValue(DrainingControl.apply)
        .run()

    // ...

    // #transactionalSink
    val (control2, result) = Consumer
      .plainSource(consumerSettings, Subscriptions.topics(sinkTopic))
      .toMat(Sink.seq)(Keep.both)
      .run()


    import org.apache.kafka.clients.producer.ProducerRecord
    //var testProducer: KProducer[String, Command] = _
    def produce(topic: String, range: immutable.Seq[AggregateObjeto.UpdateObligacion]): Future[Done] =
      Source(range)
        // NOTE: If no partition is specified but a key is present a partition will be chosen
        // using a hash of the key. If neither key nor partition is present a partition
        // will be assigned in a round-robin fashion.
        .map(n => new ProducerRecord(topic, partition0, DefaultKey, n))
        .runWith(Producer.plainSink(producerSettings))



    val obligacion = AggregateObjeto.UpdateObligacion("1", 1L, AggregateObjeto.Obligacion("1", "2", 100, DateTime.now()))
    val range = immutable.Seq(
      obligacion.copy(deliveryId = 1, obligacion = AggregateObjeto.Obligacion("2", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 2, obligacion = AggregateObjeto.Obligacion("3", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 3, obligacion = AggregateObjeto.Obligacion("3", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 4, obligacion = AggregateObjeto.Obligacion("2", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 5, obligacion = AggregateObjeto.Obligacion("1", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 6, obligacion = AggregateObjeto.Obligacion("1", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 7, obligacion = AggregateObjeto.Obligacion("2", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 8, obligacion = AggregateObjeto.Obligacion("2", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 9, obligacion = AggregateObjeto.Obligacion("3", "2", 100, DateTime.now())),
      obligacion.copy(deliveryId = 10, obligacion = AggregateObjeto.Obligacion("2", "2", 100, DateTime.now())),
    )
    awaitProduce(produce(sourceTopic, range))

    control.shutdown().futureValue should be(Done)
    control2.shutdown().futureValue should be(Done)
    // #transactionalSink
    control.drainAndShutdown()
    // #transactionalSink
    result.futureValue should have size (range.length)



    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println(result.futureValue)
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")
    println("RESULT")

    println()
    println()
    println()
    println("STATE")

    (objetoAggregateRef ? AggregateObjeto.GetState("1")).mapTo[AggregateObjeto.StateObjeto].onComplete {
      case Failure(exception) =>
      case Success(value) =>     println(value)

    }


    println()
    println()
    println()
    println()
  }



  private def probeConsumerSettings(groupId: String): ConsumerSettings[String, String] =
    consumerDefaults
      .withGroupId(groupId)
      .withProperties(ConsumerConfig.ISOLATION_LEVEL_CONFIG -> "read_committed")

  private def valuesProbeConsumer(settings: ConsumerSettings[String, String],
                                  topic: String): TestSubscriber.Probe[String] =
    offsetValueSource(settings, topic)
      .map(_._2)
      .runWith(TestSink.probe)


  private def offsetValueSource(settings: ConsumerSettings[String, String],
                                topic: String): Source[(Long, String), Consumer.Control] =
    Consumer
      .plainSource(settings, Subscriptions.topics(topic))
      .map(r => (r.offset(), r.value()))

  private def transactionalCopyStream(
                                       consumerSettings: ConsumerSettings[String, String],
                                       sourceTopic: String,
                                       sinkTopic: String,
                                       transactionalId: String,
                                       restartAfter: Int,
                                       idleTimeout: FiniteDuration
                                     ): Source[ProducerMessage.Results[String, String, PartitionOffset], Control] =
    Transactional
      .source(consumerSettings, Subscriptions.topics(sourceTopic))
      .zip(Source.unfold(1)(count => Some((count + 1, count))))
      .map {
        case (msg, count) =>
          if (count >= restartAfter) throw new Error("Restarting transactional copy stream")
          msg
      }
      .idleTimeout(idleTimeout)
      .map { msg =>
        ProducerMessage.single(new ProducerRecord[String, String](sinkTopic, msg.record.value), msg.partitionOffset)
      }
      .via(Transactional.flow(producerDefaults, transactionalId))
}