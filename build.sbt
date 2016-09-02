name := """dictwitz"""

version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := "2.11.2"

startYear := Some(2016)

resolvers in ThisBuild += Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)

libraryDependencies in ThisBuild ++= Seq(
  cache,
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0.4",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0"
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

fork in run := false

packageArchetype.java_server

mappings in Universal ++= {
  val resourcesDir = baseDirectory.value/"resources"
  for {
    file <- (resourcesDir ** AllPassFilter).get
    relative <- file.relativeTo(resourcesDir.getParentFile)
    mapping = file -> relative.getPath
  } yield mapping
}