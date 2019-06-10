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
        .enablePlugins(ScoverageSbtPlugin)
        .settings(
            // coverageEnabled := true,
            // coverageMinimum := 80,
            // coverageFailOnMinimum := true,
            addCommandAlias("testc", ";clean;coverage;test;coverageReport")
        )
        .settings(
            Test / parallelExecution := false,
            Test / fork := true,
            Test / javaOptions += "-Xmx2G"
        )
        .settings(
            triggeredMessage := Watched.clearWhenTriggered,
            // autoStartServer := false,
            // autoCompilerPlugins := true,
            shellPrompt := (_ => fancyPrompt(name.value))
        )
        .enablePlugins(JavaServerAppPackaging, DockerPlugin)
        .settings(
            dockerBaseImage := "openjdk:8",
            dockerUsername := Some("arditi")
        )

// Command Aliases
addCommandAlias("cd", "project")
addCommandAlias("ls", "projects")
addCommandAlias("to", "testOnly *")

def fancyPrompt(projectName: String): String =
  s"""|
      |[info] Welcome to the ${cyan(projectName)} project!
      |sbt> """.stripMargin

def cyan(projectName: String): String = CYAN + projectName + RESET

