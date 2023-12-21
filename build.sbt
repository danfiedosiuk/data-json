ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "data-json"
  )

val sparkVersion = "3.3.2"
val playVersion = "2.9.4"
val jsonVersion = "3.6.11"

libraryDependencies ++= Seq(
  "org.apache.spark"  %% "spark-core"    % sparkVersion,
  "org.apache.spark"  %% "spark-sql"     % sparkVersion,
  "com.typesafe.play" %% "play-json"     % playVersion,
  "org.json4s"        %% "json4s-native" % jsonVersion
)