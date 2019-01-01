name := "TV Titler"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:postfixOps",
    "-language:reflectiveCalls",
    "-unchecked"
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.6"
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.1"
libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.1.1"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-core" % "3.4.1"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.4.1"

libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

mainClass in assembly := Some("com.worthlesscog.tv.Titler")
test in assembly := {}
