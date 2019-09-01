package utils.generators

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object Infrastructure {
  val r = new scala.util.Random(100)
  def randomAvailablePort = 2551 + r.nextInt(1000)


  def randomActorSystem(randomAvailablePort: Int) = {
    val customConf =
      ConfigFactory.parseString(
        s"""
         akka {
           cluster {
             seed-nodes = ["akka://ClusterArditi-${randomAvailablePort}@127.0.0.1:$randomAvailablePort"]
           }

           remote {
              artery {
                canonical.port = $randomAvailablePort # 2551
              }
           }
        }
        """)
    val config = customConf.withFallback(ConfigFactory.load()).resolve()
    lazy val system = ActorSystem(s"ClusterArditi-${randomAvailablePort}", config)
    system
  }
}
