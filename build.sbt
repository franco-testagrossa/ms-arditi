import Settings._
import scoverage.ScoverageSbtPlugin
import scala.Console._

lazy val commonSettings = Seq(
    organization := "com.arditi",
    name := "ms-arditi",
    version := "1.0"
)

lazy val app = 
    (project in file ("./app"))
        .settings(commonSettings)
        .settings(modulesSettings)
        .settings(name := "app")
        .settings(
            fork in run := true,
            mainClass in (Compile, run) := Some("runner.Main"),
            addCommandAlias("app", "app/run")
        )
        .enablePlugins(ScoverageSbtPlugin)
        .settings(
            coverageEnabled := true,
            coverageMinimum := 80,
            coverageFailOnMinimum := true,
            addCommandAlias("testc", ";clean;coverage;test;coverageReport")
        )
        .settings(
            Test / parallelExecution := false,
            Test / fork := false,
            Test / javaOptions += "-Xmx2G"
        )
        .settings(
            triggeredMessage := Watched.clearWhenTriggered,
            autoStartServer := false,
            autoCompilerPlugins := true,
            shellPrompt := (_ => fancyPrompt(name.value))
        )

lazy val root = (project in file("."))
    .settings(commonSettings)
    .enablePlugins(ScoverageSbtPlugin)
    .settings(
        coverageEnabled := true,
        coverageMinimum := 80,
        coverageFailOnMinimum := true,
        addCommandAlias("testc", ";clean;coverage;test;coverageReport")
    )
    .settings(
        autoStartServer := false,
        shellPrompt := (_ => fancyPrompt(name.value)),
        initialCommands in console :=
            s"""| // TODO Add common imports
                | import scala.Console._
                | println("")
                | println(YELLOW + "Hello my friend," + RESET)
                | println(GREEN + "please enjoy and have fun!" + RESET)
            """.stripMargin,
    )
    .enablePlugins(JavaServerAppPackaging, DockerPlugin)
    .settings(
        dockerBaseImage := "openjdk:8",
        dockerUsername := Some("softwaremill")
    )
    .aggregate(app)

// Command Aliases
addCommandAlias("cd", "project")
addCommandAlias("ls", "projects")
addCommandAlias("to", "testOnly *")

def fancyPrompt(projectName: String): String =
  s"""|
      |[info] Welcome to the ${cyan(projectName)} project!
      |sbt> """.stripMargin

def cyan(projectName: String): String = CYAN + projectName + RESET