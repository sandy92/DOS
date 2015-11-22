name := "Facebook API"
version := "1.0"
scalaVersion := "2.11.7"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "spray repo" at "http://repo.spray.io"
//logLevel := Level.Warn
//scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
    val akkaV = "2.3.9"
    val sprayV = "1.3.3"
    Seq(
        "io.spray"            %%  "spray-can"     % sprayV,
        "io.spray"            %%  "spray-routing" % sprayV,
        "io.spray"            %%  "spray-testkit" % sprayV  % "test",
        "io.spray"              %%  "spray-json" % "1.3.2",
        "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
        "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
        "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
       )
}

Revolver.settings
