package scaladsl.CQRS

import akka.cluster.Cluster
import common.cassandra.utils.Startup
import common.cqrs.utils.ReadOrientedActorSystem
import common.cqrs.EventProcessorWrapper

object Read extends App with ReadOrientedActorSystem {
  import common.cassandra.CassandraDefaults._

  private val system = createActorSystem()
  val selfRoles = Cluster(system).selfRoles
  if (selfRoles.contains("read-model")) {
    createTables(system)
    EventProcessorWrapper(system).start()
  }

}
