import Settings._
import scoverage.ScoverageSbtPlugin
import scala.Console._

lazy val commonSettings = Seq(
    organization := "com.arditi",
    name := "ms_arditi",
    version := "1.0"
)

lazy val ms_arditi = 
    (project in file ("."))
        .settings(commonSettings)
        .settings(modulesSettings)
        .settings(
            fork in run := true,
            mainClass in (Compile, run) := Some("Main"),
            addCommandAlias("arditi", "run")
        )
        .settings(
          scalacOptions ++= Seq(
            "-feature",
            "-unchecked",
            "-language:higherKinds",
            "-language:postfixOps",
            "-deprecation"
          )
        )
        .enablePlugins(ScoverageSbtPlugin)
        .settings(
            coverageMinimum := 1,
            coverageFailOnMinimum := true,
            addCommandAlias("testc", 
                ";'set coverageEnabled := true';clean;coverage;test;coverageReport"
            )
        )
        .settings(
            Test / parallelExecution := false,
            Test / fork := true,
            Test / javaOptions += "-Xmx2G"
        )
        .settings(
            triggeredMessage := Watched.clearWhenTriggered,
            autoStartServer := false,
            shellPrompt := (_ => fancyPrompt(name.value))
        )
        .enablePlugins(JavaServerAppPackaging, DockerPlugin)
        .settings(
            dockerBaseImage := "openjdk:8",
            dockerUsername := Some("arditi")
        )



mainClass in Compile := Some("poc.MainPoc")
// mainClass in Compile := Some("com.elleflorio.cluster.playground.Server")
dockerBaseImage := "java"
version in Docker := "latest"
dockerExposedPorts := Seq(8000, 8001)
dockerEntrypoint := Seq("/opt/docker/bin/ms_arditi")


// Command Aliases
addCommandAlias("cd", "project")
addCommandAlias("ls", "projects")
addCommandAlias("to", "testOnly *")

addCommandAlias("I_demand_an_explanation_for_the_naming_convention_of_the_command_aliases",  
                """eval "Star wars references https://youtu.be/6v5VahaEL7s" """)
addCommandAlias("red_1_standing_by",   akkaStartup(lead = true, 1)) 
addCommandAlias("red_2_standing_by",   akkaStartup(lead = false, 2)) 
addCommandAlias("red_3_standing_by",   akkaStartup(lead = false, 3)) 
addCommandAlias("r0",   "red_1_standing_by") 
addCommandAlias("r1",   "red_2_standing_by") 
addCommandAlias("r2",   "red_3_standing_by")

addCommandAlias("start_cassandra", "runMain scaladsl.CQRS.App cassandra ")
addCommandAlias("read_cassandra",   cassandraExperiment(lead = true,   1, role = "read"))
addCommandAlias("write_cassandra",  cassandraExperiment(lead = false,  2, role = "write"))

def cassandraExperiment(lead: Boolean, i: Int, role: String): String = 
s"""runMain scaladsl.CQRS.App 255$i
    | ${commonFlags(lead, i)}
    | -Dakka.cluster.roles.0=${role}-model
    | -Dakka.persistence.journal.plugin=cassandra-journal
    | -Dakka.persistence.snapshot-store.plugin=cassandra-snapshot-store
    |""".stripMargin

def akkaStartup(lead: Boolean, i: Int): String = 
 "reStart " + commonFlags(lead, i)

def commonFlags(lead: Boolean, i: Int): String = 
s"""|  
   |---
   |-Dapplication.api.host=127.0.0.$i
   |-Dapplication.api.port=8080
   |-Dakka.cluster.seed-nodes.0=akka://ClusterArditi@127.0.0.1:2551
   |-Dakka.cluster.roles.0=${if (lead) "static" else "dynamic"}
   |-Dakka.discovery.method=config
   |-Dakka.management.http.hostname=127.0.0.$i
   |-Dakka.remote.artery.canonical.hostname=127.0.0.$i
   |""".stripMargin
   

   
def fancyPrompt(projectName: String): String =
  s"""|
      |[info] Welcome to the ${cyan(projectName)} project!
      |sbt> """.stripMargin

def cyan(projectName: String): String = CYAN + projectName + RESET

// Experimental
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-M1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-typed" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.5.23"



libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka-testkit" % "1.0.5"


