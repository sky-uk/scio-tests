//import sbt._
//import sbt.Keys._
import ReleaseKeys._
import ReleaseTransformations._

val scioVersion = "0.7.0"
val beamVersion = "2.9.0"
val scalaMacrosVersion = "2.1.1"

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

dockerBaseImage := "gcr.io/sky-italia-bigdata/oracle-jdk:jdk-1.8"

lazy val root: Project = project
  .in(file("."))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scio-tests",
    description := "scio-tests",
    publish / skip := true,
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.1",
      "com.spotify" %% "scio-core" % scioVersion,
      "com.spotify" %% "scio-avro" % scioVersion,
      "com.spotify" %% "scio-parquet" % scioVersion,
      "com.spotify" %% "scio-test" % scioVersion % Test,
      "org.apache.beam" % "beam-runners-direct-java" % beamVersion,
      // optional dataflow runner
      "org.apache.beam" % "beam-runners-google-cloud-dataflow-java" % beamVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.25",
      "org.scalatest" %% "scalatest" % "3.0.6" % "it,test"
    ),
    commands ++= Seq(command)
  )

lazy val runITAction = { st: State =>
    if (!st.get(skipTests).getOrElse(false)) {
      val extracted = Project.extract(st)
      val ref = extracted.get(thisProjectRef)
      extracted.runAggregated(test in IntegrationTest in ref, st)
    } else st
  }

lazy val runIntegrationTest: ReleaseStep = ReleaseStep(
  action = runITAction,
  enableCrossBuild = true
)

lazy val command: Command = Command.command("runMyITTests")(runITAction)

lazy val dockerPublishLocal: ReleaseStep = ReleaseStep(
  action = runITAction,
  enableCrossBuild = true
)



releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  runIntegrationTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  dockerPublishLocal,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
