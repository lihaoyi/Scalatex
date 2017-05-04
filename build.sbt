

publishArtifact := false

publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))

crossScalaVersions:= Seq("2.11.8", "2.12.1")

lazy val Version = new {
  def scalaTags = "0.6.2"
  def upickle = "0.4.4"
}

val sharedSettings = Seq(
  version := _root_.scalatex.Constants.version,
  organization := "com.lihaoyi",
  crossScalaVersions:= Seq("2.11.8", "2.12.1"),
  scalaVersion := "2.12.1",
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
      <scm>
        <url>git://github.com/lihaoyi/Scalatex.git</url>
        <connection>scm:git://github.com/lihaoyi/Scalatex.git</connection>
      </scm>
      <developers>
        <developer>
          <id>lihaoyi</id>
          <name>Li Haoyi</name>
          <url>https://github.com/lihaoyi</url>
        </developer>
      </developers>
)

lazy val api = project.settings(sharedSettings:_*)
  .settings(
    name := "scalatex-api",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.4.4" % "test",
      "com.lihaoyi" %% "scalaparse" % "0.4.2",
      "com.lihaoyi" %% "scalatags" % Version.scalaTags,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalatexSbtPlugin = project.settings(sharedSettings:_*)
  .settings(
  name := "scalatex-sbt-plugin",
  scalaVersion := "2.10.6",
  sbtPlugin := true,
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"Constants.scala"
)

lazy val site =
  project
    .enablePlugins(ScalaJSPlugin)
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
    "com.lihaoyi" %% "scalatags" % Version.scalaTags,
    "org.webjars" % "pure" % "0.6.0",
    "com.lihaoyi" %% "upickle" % Version.upickle,
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
    scalaVersion := "2.12.1",
    crossScalaVersions:= Seq("2.11.8", "2.12.1"),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % Version.upickle,
      "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      "com.lihaoyi" %%% "scalatags" % Version.scalaTags
    ),
    emitSourceMaps := false,

    publishArtifact := false,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

lazy val readme = scalatex.ScalatexReadme(
  projectId = "readme",
  wd = file(""),
  url = "https://github.com/lihaoyi/scalatex/tree/master",
  source = "Readme"
)
.settings(
  libraryDependencies := libraryDependencies.value.filter(_.name == "scalatex-site"),
  (unmanagedSources in Compile) += baseDirectory.value/".."/"project"/"Constants.scala",
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)
.dependsOn(
  site
)
