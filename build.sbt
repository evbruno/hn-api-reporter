ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val akkaVersion = "2.6.19"
val akkaHttpVersion = "10.2.9"

lazy val root = (project in file("."))
  .settings(
    name := "hn-reporter",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.12" % "test",

      "com.softwaremill.sttp.client3" %% "core" % "3.7.1",
      "com.softwaremill.sttp.client3" %% "circe" % "3.7.1",
      "io.circe" %% "circe-generic" % "0.14.2"
    )
  )


