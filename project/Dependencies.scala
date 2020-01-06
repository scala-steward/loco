import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.0" % Test
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.4.0" % Test

  lazy val mysql = "mysql" % "mysql-connector-java" % "8.0.15" % Test
  lazy val embeddedMysql = "com.wix" % "wix-embedded-mysql" % "4.2.0" % Test

  lazy val doobieCore = "org.tpolecat" %% "doobie-core" % "0.8.8"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.0.0"
  lazy val fs2Core = "co.fs2" %% "fs2-core" % "2.1.0"
  lazy val fs2Reactive = "co.fs2" %% "fs2-reactive-streams" % "2.1.0"

  lazy val jsoniter = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.1.2"
  lazy val jsoniterMacros = "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.1.2"

  lazy val mongodbReactiveStreams = "org.mongodb" % "mongodb-driver-reactivestreams" % "1.13.0"

  lazy val mongodbEmbedded = "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.2.0" % Test
}