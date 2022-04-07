import Dependencies._
import ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runClean,                               // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

val common = List(
  organization := "loco",
  scalaVersion := "2.13.6",
)
inThisBuild(common)



val publishing = List(
  organization := "com.yarhrn",
  homepage := Some(url("https://github.com/yarhrn/loco")),
  scmInfo := Some(ScmInfo(url("https://github.com/yarhrn/loco"), "git@github.com:yarhrn/loco.git")),
  developers := List(Developer("Yaroslav Hryniuk",
    "Yaroslav Hryniuk",
    "yaroslavh.hryniuk@gmail.com",
    url("https://github.com/yarhrn"))),
  licenses += ("MIT", url("https://github.com/yarhrn/loco/blob/master/LICENSE")),
  publishMavenStyle := true
)

lazy val noPublishing = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  skip in publish := true,
  publishTo := Some("dummy" at "nowhere"),
)

lazy val root = project
  .settings(noPublishing)
  .in(file("."))
  .aggregate(
    core,
    doobie,
    example
  )

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      scalaTest,
      scalaMock,
      fs2Core,
      catsEffect,
      jsoniter,
      jsoniterMacros,
      catsEffectStd
    ),
    publishing
  )


//lazy val mongodb = (project in file("mongodb"))
//  .settings(
//    name := "mongodb",
//    libraryDependencies ++= Seq(mongodbEmbedded, mongodbReactiveStreams, fs2Reactive),
//    publishing
//  ).dependsOn(core % "test->test;compile->compile")

lazy val example = (project in file("example")).
  settings(
    name := "example",
    libraryDependencies ++= Seq(scalaTest, scalaMock),
    noPublishing
  ).dependsOn(core % "test->test;compile->compile")
/*
lazy val laws = (project in file("laws")).
  settings(
    name := "laws",
    libraryDependencies ++= Seq(scalaTest,
      scalaMock,
      "org.typelevel" %% "discipline-core" % "1.1.5",
      "org.typelevel" %% "cats-laws" % "2.1.0"),
    noPublishing
  ).dependsOn(core % "test->test;compile->compile")
*/
lazy val doobie = (project in file("doobie"))
  .settings(
    common,
    name := "doobie",
    libraryDependencies ++= Seq(doobieCore, scalaTest, scalaMock, postgresql, embeddedPostgresql),
    publishing
  ).dependsOn(core % "test->test;compile->compile")