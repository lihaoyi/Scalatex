unmanagedSources in Compile ++= {
  val root = baseDirectory.value.getParentFile
  val exclude = Map(
    "0.13" -> "1.0",
    "1.0" -> "0.13"
  ).apply(sbtBinaryVersion.value)
  (root / "scalatexSbtPlugin")
    .descendantsExcept("*.scala", s"*sbt-$exclude*")
    .get
}

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.31")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")


