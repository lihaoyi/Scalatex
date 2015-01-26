unmanagedSources in Compile ++= {
  val root = baseDirectory.value.getParentFile
  (root / "scalatexSbtPlugin" ** "*.scala").get
}
