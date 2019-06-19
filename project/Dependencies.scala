import sbt._
import sbt.Keys._

object Dependencies {
    // Versions
    private lazy val scalaVersion = "2.12.8"

    // Resolvers
    lazy val commonResolvers = Seq(
        Resolver sonatypeRepo "public",
        Resolver typesafeRepo "releases",
        Resolver.bintrayRepo("tanukkii007", "maven"),
        // the library is available in Bintray repository
        ("dnvriend" at "http://dl.bintray.com/dnvriend/maven")
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

    object TestDB extends Module {
        private lazy val lvlDbVersion = "0.9"
        private lazy val lvlDbJniVersion = "1.8"

        private lazy val lvlDb = "org.iq80.leveldb" % "leveldb" % lvlDbVersion
        private lazy val lvlDbJni = "org.fusesource.leveldbjni" % "leveldbjni-all" % lvlDbJniVersion
        
        override def modules: Seq[ModuleID] = 
            ("com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.15.1") :: Nil
    }
    
    object Akka extends Module {
        private lazy val akkaVersion = "2.5.23"
        private lazy val akkaHttpVersion = "10.1.7"
        private lazy val akkaManagementVersion = "1.0.0"
        private lazy val AkkaPersistenceCassandraVersion = "0.93"

        private def akkaModule(name: String) = "com.typesafe.akka" %% name % akkaVersion 
        private def akkaHttpModule(name: String) = "com.typesafe.akka" %% name % akkaHttpVersion 
        private def akkaManagmentModule(name: String) = "com.lightbend.akka.management" %% name % akkaManagementVersion
        private def akkaPersistenceCassandraModule(name: String) = "com.typesafe.akka" %% name % AkkaPersistenceCassandraVersion

        override def modules: Seq[ModuleID] =
            akkaModule("akka-cluster") :: 
            akkaModule("akka-cluster-sharding") :: 
            akkaModule("akka-cluster-tools") :: 
            akkaModule("akka-remote") :: 
            akkaModule("akka-slf4j") :: 
            akkaModule("akka-discovery") :: 
            akkaModule("akka-persistence") :: 
            akkaModule("akka-persistence-query") :: 
            akkaModule("akka-actor") :: 
            akkaModule("akka-testkit") :: 
            akkaManagmentModule("akka-management") :: 
            akkaManagmentModule("akka-management-cluster-http") :: 
            akkaManagmentModule("akka-management-cluster-bootstrap") :: 
            akkaHttpModule("akka-http") ::
            akkaHttpModule("akka-http-core") ::
            akkaPersistenceCassandraModule("akka-persistence-cassandra") ::
            akkaPersistenceCassandraModule("akka-persistence-cassandra-launcher") ::
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
    lazy val testDeps = Test.modules ++ TestDB.modules
}

trait Dependencies {
    val scalaVersionUsed = Dependencies.scalaVersion
    val commonResolvers = Dependencies.commonResolvers
    val mainDeps = Dependencies.mainDeps
    val testDeps = Dependencies.testDeps
}