lazy val Constants = _root_.scalatex.Constants
sharedSettings
noPublish

lazy val sharedSettings = Seq(
  version := Constants.version,
  organization := "com.lihaoyi",
  crossScalaVersions:= Seq(Constants.scala211, Constants.scala212),
  scalaVersion := Constants.scala212,
  libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.1.5" % "provided",
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.5"),
  autoCompilerPlugins := true,
  publishTo := Some("releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
  pomExtra :=
    <url>https://github.com/lihaoyi/Scalatex</url>
      <licenses>
        <license>
          <name>MIT license</name>
          <url>http://www.opensource.org/licenses/mit-license.php</url>
        </license>
      </licenses>
      <developers>
        <developer>
          <id>lihaoyi</id>
          <name>Li Haoyi</name>
          <url>https://github.com/lihaoyi</url>
        </developer>
      </developers>
)

lazy val noPublish = Seq(
  publishArtifact := false,
  publishLocal := {},
  publish := {}
)

lazy val api = project.settings(sharedSettings:_*)
  .settings(
    name := "scalatex-api",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.4.8" % "test",
      "com.lihaoyi" %% "scalaparse" % "0.4.3",
      "com.lihaoyi" %% "scalatags" % Constants.scalaTags,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalatexSbtPlugin = project.settings(sharedSettings:_*)
  .settings(
  name := "scalatex-sbt-plugin",
  scalaVersion := {
    if (sbtVersion.in(pluginCrossBuild).value.startsWith("0.13")) Constants.scala210
    else Constants.scala212
  },
  crossScalaVersions := List(Constants.scala212),
  // scalatexSbtPlugin/publish uses sbt 1.0 by default. To publish for 0.13, run
  // ^^ 0.13.16 # similar as ++2.12.3 but for sbtVersion instead.
  // scalatexSbtPlugin/publish
  crossSbtVersions := List("1.0.0", "0.13.16"),
  sbtPlugin := true,
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"Constants.scala"
)

lazy val site =
  project
    .dependsOn(api)
    .settings(scalatex.SbtPlugin.projectSettings:_*)
    .settings(sharedSettings:_*)
    .settings(
  libraryDependencies := libraryDependencies.value.filter(!_.toString.contains("scalatex-api")),
  name := "scalatex-site",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "utest" % "0.4.4" % "test",
    "com.lihaoyi" %% "ammonite-ops" % "0.8.1",
    "org.webjars" % "highlightjs" % "9.7.0",
    "org.webjars" % "font-awesome" % "4.7.0",
    "com.lihaoyi" %% "scalatags" % Constants.scalaTags,
    "org.webjars" % "pure" % "0.6.0",
    "com.lihaoyi" %% "upickle" % Constants.upickle,
    "org.scalaj" %% "scalaj-http" % "2.3.0"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  (managedResources in Compile) += {
    val file = (resourceManaged in Compile).value/"scalatex"/"scrollspy"/"scrollspy.js"
    val js = (fullOptJS in (scrollspy, Compile)).value.data
    sbt.IO.copyFile(js, file)
    file
  }
)

lazy val scrollspy = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    sharedSettings,
    scalaVersion := Constants.scala212,
    crossScalaVersions:= Seq(Constants.scala211, Constants.scala212),
    scalacOptions += "-P:scalajs:suppressExportDeprecations", // see https://github.com/scala-js/scala-js/issues/3092
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % Constants.upickle,
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.lihaoyi" %%% "scalatags" % Constants.scalaTags
    ),
    emitSourceMaps := false,
    noPublish
  )

lazy val readme = scalatex.ScalatexReadme(
  projectId = "readme",
  wd = file(""),
  url = "https://github.com/lihaoyi/scalatex/tree/master",
  source = "Readme"
)
.settings(
  sharedSettings,
  siteSourceDirectory := target.value / "scalatex",
  git.remoteRepo := "git@github.com:lihaoyi/scalatex.git",
  libraryDependencies := libraryDependencies.value.filter(_.name == "scalatex-site"),
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"Constants.scala",
  noPublish
)
.dependsOn(
  site
)
.enablePlugins(GhpagesPlugin)
