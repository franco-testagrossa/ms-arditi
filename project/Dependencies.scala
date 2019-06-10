import sbt._
import sbt.Keys._

object Dependencies {
    // Versions
    private lazy val scalaVersion = "2.12.7"

    // Resolvers
    lazy val commonResolvers = Seq(
        Resolver sonatypeRepo "public",
        Resolver typesafeRepo "releases",
        Resolver.bintrayRepo("tanukkii007", "maven")
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
        private lazy val akkaVersion = "2.5.23"
        private lazy val akkaHttpVersion = "10.1.7"
        private lazy val akkaManagementVersion = "1.0.0"

        private def akkaModule(name: String) = "com.typesafe.akka" %% name % akkaVersion 
        private def akkaHttpModule(name: String) = "com.typesafe.akka" %% name % akkaHttpVersion 
        private def akkaManagmentModule(name: String) = "com.lightbend.akka.management" %% name % akkaManagementVersion 
        
        override def modules: Seq[ModuleID] =
            akkaModule("akka-cluster") :: 
            akkaModule("akka-cluster-sharding") :: 
            akkaModule("akka-cluster-tools") :: 
            akkaModule("akka-remote") :: 
            akkaModule("akka-slf4j") :: 
            akkaModule("akka-discovery") :: 
            akkaModule("akka-actor") :: 
            akkaModule("akka-testkit") :: 
            akkaManagmentModule("akka-management") :: 
            akkaManagmentModule("akka-management-cluster-http") :: 
            akkaManagmentModule("akka-management-cluster-bootstrap") :: 
            akkaHttpModule("akka-http") ::
            akkaHttpModule("akka-http-core") ::
            "com.github.TanUkkii007" %% "akka-cluster-custom-downing" % "0.0.12" :: // SBR
            Nil
    }
    
    object Utils extends Module {
        private lazy val logbackVersion = "1.2.3"
        private lazy val kryoVersion = "0.9.3"
        
        private lazy val logback = "ch.qos.logback" % "logback-classic" % logbackVersion
        private lazy val kryo = "com.twitter" %% "chill-akka" % kryoVersion

        override def modules: Seq[ModuleID] = logback :: kryo :: Nil
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