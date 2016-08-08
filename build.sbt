name := """bookwitz"""

organization in ThisBuild := "io.bookwitz"

version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := "2.11.2"

startYear := Some(2013)

homepage := Some(url("https://blablabla"))

licenses := Seq("GNU AFFERO GENERAL PUBLIC LICENSE, Version 3" -> url("http://www.gnu.org/licenses/agpl-3.0.txt"))

resolvers in ThisBuild += Resolver.url("Edulify Repository", url("http://edulify.github.io/modules/releases/"))(Resolver.ivyStylePatterns)

libraryDependencies in ThisBuild ++= Seq(
  cache,
  jdbc,
  "javax.inject" % "javax.inject" % "1",
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0.4",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0.4",
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "com.edulify" %% "play-hikaricp" % "1.4.1",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0"
)

libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

// http://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.8.11.2"


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

unmanagedJars in Compile ++= {
  val base = baseDirectory.value
  val baseDirectories = (base / "lib")
  val customJars = (baseDirectories ** "*.jar")
  customJars.classpath
}

herokuAppName in Compile := "still-plains-63986"