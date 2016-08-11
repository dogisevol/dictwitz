name := """dictwitz"""

organization in ThisBuild := "io.dictwitz"

version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := "2.11.2"

startYear := Some(2016)

homepage := Some(url("https://still-plains-63986.herokuapp.com/"))

resolvers in ThisBuild += Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)

libraryDependencies in ThisBuild ++= Seq(
  cache,
  //jdbc,
  //"javax.inject" % "javax.inject" % "1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0.4",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

// http://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
//libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.11.2"


pipelineStages := Seq(rjs, digest, gzip)

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  //"-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-language:reflectiveCalls"
)

fork in run := false

packageArchetype.java_server

// add your config files to the classpath for running inside sbt
unmanagedClasspath in Compile += Attributed.blank(sourceDirectory.value/"main"/"config")

mappings in Universal ++= {
  val resourcesDir = baseDirectory.value/"resources"
  for {
    file <- (resourcesDir ** AllPassFilter).get
    relative <- file.relativeTo(resourcesDir.getParentFile)
    mapping = file -> relative.getPath
  } yield mapping
}


herokuAppName in Compile := "still-plains-63986"

herokuProcessTypes in Compile := Map(
  "web" -> "target/universal/stage/bin/"
)

herokuIncludePaths in Compile := Seq(
  "target/universal/stage"
)