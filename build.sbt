name := "Hello Test #1"
version := "1.0"
scalaVersion := "2.10.0"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
libraryDependencies += "com.typesafe.akka" % "akka-actor_2.10" % "2.2-M1"
mainClass in (Compile,run) := Some("Bitcoin")
