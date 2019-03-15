import sbt._
import sbt.Keys._
import ReleaseKeys._
import ReleaseTransformations._

val scioVersion = "0.7.0"
val beamVersion = "2.9.0"
val scalaMacrosVersion = "2.1.1"

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
publishTo := {
      val nexus = "http://maven.skytv.it/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "repository/maven-snapshots")
      else
        Some("releases" at nexus + "repository/maven-releases")
    }

credentials += Credentials("Sonatype Nexus Repository Manager", "maven.skytv.it", "admin", "admin123")

dockerBaseImage := "gcr.io/sky-italia-bigdata/oracle-jdk:jdk-1.8"
dockerRepository := Some("gcr.io")
dockerUsername := Some("sky-italia-bigdata")
packageName in Docker := "test-project"

dockerAliases ++= Seq(
  dockerAlias.value.withTag(Option(
    s"${version.value}-${git.gitCurrentBranch.value}"
  ))
)

lazy val root: Project = project
  .in(file("."))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "scio-tests",
    description := "scio-tests",
    //publish / skip := true,
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
    commands ++= Seq(myTests, myDocker)
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

lazy val myTests: Command = Command.command("runMyITTests")(runITAction)

lazy val dockerPublishAction = { st: State =>
  val extracted = Project.extract(st)
  val ref = extracted.get(thisProjectRef)
  extracted.runAggregated(publish in Docker in ref, st)
}

lazy val dockerPublish: ReleaseStep = ReleaseStep(
  action = dockerPublishAction,
  enableCrossBuild = true
)

lazy val myDocker: Command = Command.command("myDockerPublish")(dockerPublishAction)

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
  dockerPublish,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
