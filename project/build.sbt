unmanagedSources in Compile ++= {
  val root = baseDirectory.value.getParentFile
  (root / "scalatexSbtPlugin" ** "*.scala").get
}

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.11")