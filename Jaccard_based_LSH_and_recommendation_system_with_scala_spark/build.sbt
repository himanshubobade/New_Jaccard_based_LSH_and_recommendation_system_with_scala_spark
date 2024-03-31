ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10"

val sparkVersion = "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "Assignment1",
    // Define merge strategy
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql"  % sparkVersion
)

// Import sbt-assembly plugin settings
enablePlugins(AssemblyPlugin)

// Define sbt-assembly settings
assembly / assemblyJarName := "Assignment1.jar"
