package scaladsl.CQRS

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import common.cqrs.utils.WriteOrientedActorSystem
import common.cqrs.{ ShardedEntity }
import scaladsl.CQRS.example.ExampleEntity

import scala.concurrent.duration._

object Write extends App with ShardedEntity with WriteOrientedActorSystem {

  override val entityProps: Props = ExampleEntity.props

  private val system = createActorSystem()

  val selfRoles = Cluster(system).selfRoles
  if (selfRoles.contains("write-model")) {
    ShardedEntity(system).start()
    testIt(system)
  }

  def testIt(system: ActorSystem): Unit = {

    val shardedSwitch = ShardedEntity(system)

    shardedSwitch ! ("switch", ExampleEntity.CreateSwitch(4))
    shardedSwitch ! ("switch1", ExampleEntity.SetPortStatus(2, portEnabled = true))
    shardedSwitch ! ("switch1", ExampleEntity.SendPortStatus)

    shardedSwitch ! ("switch2", ExampleEntity.CreateSwitch(6))
    shardedSwitch ! ("switch2", ExampleEntity.SetPortStatus(0, portEnabled = true))
    shardedSwitch ! ("switch2", ExampleEntity.SetPortStatus(2, portEnabled = true))
    shardedSwitch ! ("switch2", ExampleEntity.SetPortStatus(5, portEnabled = true))
    shardedSwitch ! ("switch2", ExampleEntity.SendPortStatus)

    val counter = new AtomicInteger(8)
    import system.dispatcher
    system.scheduler.schedule(3.seconds, 1.second) {
      import scala.util.Random
      val switch = s"switch${counter.getAndIncrement()}"
      shardedSwitch ! (switch, ExampleEntity.CreateSwitch(6))
      shardedSwitch ! (switch, ExampleEntity.SetPortStatus(Random.nextInt(6), portEnabled = true))
      shardedSwitch ! (switch, ExampleEntity.SetPortStatus(Random.nextInt(6), portEnabled = true))
      shardedSwitch ! (switch, ExampleEntity.SendPortStatus)
    }

  }

}
