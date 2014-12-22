import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
val sharedSettings = Seq(
  version := scalatex.SbtPlugin.scalatexVersion,
  organization := "com.lihaoyi",
  crossScalaVersions:= Seq("2.10.4", "2.11.2"),
  scalaVersion := "2.11.4",
  libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
  autoCompilerPlugins := true
)

lazy val scalaParser = project.settings(sharedSettings:_*)
  .settings(
    name := "scala-parser-lite",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.2.4",
      "org.parboiled" %% "parboiled" % "2.0.1"
    ),
    testFrameworks += new TestFramework("utest.runner.JvmFramework")
  )
lazy val api = project.settings(sharedSettings:_*)
  .dependsOn(scalaParser)
  .settings(
    name := "scalatex-api",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.2.4",
      "com.scalatags" %% "scalatags" % "0.4.2",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.parboiled" %% "parboiled" % "2.0.1"
    ),
    testFrameworks += new TestFramework("utest.runner.JvmFramework")
  )

lazy val scalatexSbtPlugin = project.settings(sharedSettings:_*)
  .settings(
  name := "scalatex-sbt-plugin",
  scalaVersion := "2.10.4",
  sbtPlugin := true
)

lazy val siteClient =
  project.settings(scalaJSSettings ++ sharedSettings:_*).settings(
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.5",
    libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
    libraryDependencies += "com.scalatags" %%% "scalatags" % "0.4.2"
  )

lazy val site =
  project
    .dependsOn(api)
    .settings(sharedSettings:_*)
    .settings(
  name := "scalatex-site",
  libraryDependencies ++= Seq(
    "org.webjars" % "highlightjs" % "8.2-1",
    "org.webjars" % "font-awesome" % "4.2.0",
    "org.webjars" % "pure" % "0.5.0"
  ),
  (resources in Compile) += {
    (fullOptJS in (siteClient, Compile)).value
    (artifactPath in (siteClient, Compile, fullOptJS)).value
  }
)

lazy val readme = project
  .settings(scalatex.SbtPlugin.projectSettings:_*)
  .dependsOn(api, site)
  .settings(
  libraryDependencies := libraryDependencies.value.filter(!_.toString.contains("scalatex-api")),
  scalaVersion := "2.11.4"
)