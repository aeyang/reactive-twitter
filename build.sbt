name := """twitter-stream"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Bug in async-http-client that plagues Play 2.4.x breaks OAuth when request contains characters that
// need to be encoded.
libraryDependencies += "com.ning" % "async-http-client" % "1.9.29"

// use play-extra-iteratees to decode bytes into UTF-8 and to take care of chunks of Array[Byte]
resolvers += "Typesage private" at "https://private-repo.typesafe.com/typesafe/maven-releases"
libraryDependencies += "com.typesafe.play.extras" %% "iteratees-extras" % "1.5.0"