addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.5.5")

addSbtPlugin("com.lihaoyi" % "utest-js-plugin" % "0.2.4")

unmanagedSources in Compile ++= {
  val root = baseDirectory.value.getParentFile
  (root / "scalatexSbtPlugin" ** "*.scala").get
}
