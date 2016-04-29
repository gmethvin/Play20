// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

val sbtNativePackagerVersion = "1.0.3"
val sbtTwirlVersion = sys.props.getOrElse("twirl.version", "1.1.1")

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackagerVersion,
  "sbtTwirlVersion" -> sbtTwirlVersion
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.getOrElse("interplay.version", "1.1.2"))
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % sbtTwirlVersion)
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.8")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value,
  "org.webjars" % "webjars-locator-core" % "0.26"
)

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")
