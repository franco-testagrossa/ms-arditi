import sbt._
import sbt.Keys._

object Dependencies {
    // Versions
    private lazy val scalaVersion = "2.12.7"

    // Resolvers
    lazy val commonResolvers = Seq(
        Resolver sonatypeRepo "public",
        Resolver typesafeRepo "releases"
    )

    // Module
    trait Module {
        def modules: Seq[ModuleID]
    }
    object Test extends Module {
        private lazy val scalaTestVersion = "3.0.5"
        private lazy val scalaCheckVersion = "1.14.0"
        
        private lazy val scalaTic = "org.scalactic" %% "scalactic" % scalaTestVersion
        private lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
        private lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
        
        override def modules: Seq[ModuleID] = scalaTest :: scalaTic :: scalaCheck :: Nil
    }
    
    object Akka extends Module {
        private lazy val akkaVersion = "2.5.21"
        private def module(name: String) = "com.typesafe.akka" %% name % akkaVersion 
        
        override def modules: Seq[ModuleID] =
            module("akka-actor") :: 
            module("akka-slf4j") :: 
            module("akka-testkit") :: 
            Nil
    }
    
    object Utils extends Module {
        private lazy val logbackVersion = "1.2.3"
        private lazy val playJsonVersion = "2.6.9"
        
        private lazy val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
        private lazy val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

        override def modules: Seq[ModuleID] = logback :: playJson :: Nil
    }

    // Projects
    lazy val mainDeps = Akka.modules ++ Utils.modules
    lazy val testDeps = Test.modules
}

trait Dependencies {
    val scalaVersionUsed = Dependencies.scalaVersion
    val commonResolvers = Dependencies.commonResolvers
    val mainDeps = Dependencies.mainDeps
    val testDeps = Dependencies.testDeps
}