import Settings._
import scoverage.ScoverageSbtPlugin

import scala.Console._

lazy val commonSettings = Seq(
  organization := "com.arditi",
  name := "ms_arditi",
  version := "1.0"
)

lazy val ms_arditi =
  (project in file("."))
    .settings(commonSettings)
    .settings(modulesSettings)
    .settings(
      fork in run := true,
      mainClass in(Compile, run) := Some("Main"),
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
      Test / parallelExecution := true,
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
      dockerUsername := Some("ms_arditi"),
      dockerEntrypoint := Seq("/opt/docker/bin/main"),
      dockerExposedPorts := Seq(8080, 8558)
    )

// Command Aliases
addCommandAlias("cd", "project")
addCommandAlias("ls", "projects")
addCommandAlias("to", "testOnly *")
addCommandAlias("main", "run -Dakka.cluster.seed-nodes.0=akka://ClusterArditi@127.0.0.1:2551")
addCommandAlias("produce", "runMain transaction.ProducerApp")


def cyan(projectName: String): String = CYAN + projectName + RESET

def fancyPrompt(projectName: String): String =
  s"""|
      |[info] Welcome to the ${cyan(projectName)} project!
      |sbt> """.stripMargin


// Experimental
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-M1"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka-testkit" % "1.0.5"

val circeVersion = "0.11.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


