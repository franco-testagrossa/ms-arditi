/*
 * Copyright (C) 2014 - 2016 Softwaremill <http://softwaremill.com>
 * Copyright (C) 2016 - 2019 Lightbend Inc. <http://www.lightbend.com>
 */


import java.util.concurrent.atomic.AtomicReference

import akka.{Done, RestartActorSupervisorFactory, ShardedEntity}
import akka.actor.{ActorLogging, ActorRef, Kill, Props}
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.scaladsl.{Consumer, Transactional}
import akka.kafka.testkit.scaladsl.EmbeddedKafkaLike
import akka.kafka.{ConsumerMessage, ProducerMessage, Subscriptions}
import akka.pattern.ask
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import akka.util.Timeout
import model.ddd.{Command, Event}
import model.product.ProductActor.StateProduct
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class TransactionsExample extends DocsSpecBase{


  import TransactionsExample._

  override def sleepAfterProduce: FiniteDuration = 10.seconds



  def example(performKilling: Boolean,
              actorRef: ActorRef) = {
    val consumerSettings = consumerDefaults.withGroupId(createGroupId())
    val producerSettings = producerDefaults
    val sourceTopic = createTopic(1)
    val sinkTopic = createTopic(2)
    val transactionalId = createTransactionalId()
    // #transactionalFailureRetry
    val innerControl = new AtomicReference[Control](Consumer.NoopControl)


    type Msg = ConsumerMessage.TransactionalMessage[String, String]
    var acc = 0
    val stream =
      Transactional
        .source(consumerSettings, Subscriptions.topics(sourceTopic))
        .mapAsync(1) {
          msg: ConsumerMessage.TransactionalMessage[String, String] =>
            acc += 1
            if (performKilling && acc % 3 == 0) actorRef ! Kill
            val msgCommand = MockCommand(message = msg.record.value)
            (actorRef ? msgCommand) (Timeout(10 seconds)).mapTo[String].map((msg, _))(ec)


        }
        .map {
          case (msg: Msg, output: String) =>

            ProducerMessage.single(new ProducerRecord(sinkTopic, msg.record.key, output), msg.partitionOffset)
        }
        // side effect out the `Control` materialized value because it can't be propagated through the `RestartSource`
        .mapMaterializedValue(c => innerControl.set(c))
        .via(Transactional.flow(producerSettings, transactionalId))


    stream.runWith(Sink.ignore)

    // Add shutdown hook to respond to SIGTERM and gracefully shutdown stream
    sys.ShutdownHookThread {
      Await.result(innerControl.get.shutdown(), 30.seconds)
    }
    // #transactionalFailureRetry
    val (control2, result) = Consumer
      .plainSource(consumerSettings, Subscriptions.topics(sinkTopic))
      .toMat(Sink.seq)(Keep.both)
      .run()

    awaitProduce(produce(sourceTopic, 1 to 10))
    innerControl.get.shutdown().futureValue should be(Done)
    control2.shutdown().futureValue should be(Done)
    result.futureValue should have size (10)
  }


  "Supervisor" should "work, easily" in assertAllStagesStopped {
    val supervisor = new RestartActorSupervisorFactory()
    example(
      performKilling = false,
      supervisor.create(Props(new ProductMock), "Product")
    )
  }

  "SupervisedSharding" should "work, easily" in assertAllStagesStopped {
    example(
      performKilling = false,
      ProductMock.start
    )
  }
  "Supervisor + Killing" should "work, because supervisor works." in assertAllStagesStopped {
    val supervisor = new RestartActorSupervisorFactory()
    example(
      performKilling = true,
      supervisor.create(Props(new ProductMock), "Product")
    )
  }


}

object TransactionsExample {


  final case class MockCommand(
                                aggregateRoot: String = "1.0",
                                deliveryId: BigInt = 1L,
                                message: String) extends Command

  class ProductMock extends PersistentActor with ActorLogging {

    import ProductMock._

    private var state: StateProduct = StateProduct.init()

    override def receiveCommand: Receive = {
      case MockCommand(aggregateRoot, _, _) => sender() ! aggregateRoot
    }

    override def persistenceId: String = typeName + "-" + self.path.name

    override def receiveRecover: Receive = {
      case evt: Event =>
        log.info(s"replay event: $evt")
        state += evt
      case SnapshotOffer(_, snapshot: StateProduct) =>
        state = snapshot
    }
  }

  object ProductMock extends ShardedEntity {

    val typeName = "FailableProduct"

    def props(): Props = Props(new ProductMock)


  }

}