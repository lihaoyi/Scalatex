
val sharedSettings = Seq(
  version := scalatex.SbtPlugin.scalatexVersion,
  organization := "com.lihaoyi",
  crossScalaVersions:= Seq("2.10.4", "2.11.4"),
  scalaVersion := "2.11.4",
  libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided",
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
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

lazy val scalaParser = project.settings(sharedSettings:_*)
  .settings(
    name := "scala-parser-lite",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.2.4" % "test",
      "org.parboiled" %% "parboiled" % "2.0.1"
    ),
    testFrameworks += new TestFramework("utest.runner.JvmFramework")
  )
lazy val api = project.settings(sharedSettings:_*)
  .dependsOn(scalaParser)
  .settings(
    name := "scalatex-api",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % "0.2.4" % "test",
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

lazy val site =
  project
    .dependsOn(api)
    .settings(scalatex.SbtPlugin.projectSettings:_*)
    .settings(sharedSettings:_*)
    .settings(
  libraryDependencies := libraryDependencies.value.filter(!_.toString.contains("scalatex-api")),
  name := "scalatex-site",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "utest" % "0.2.4" % "test",
    "com.lihaoyi" %% "ammonite" % "0.1.4",
    "org.webjars" % "highlightjs" % "8.2-1",
    "org.webjars" % "font-awesome" % "4.2.0",
    "org.webjars" % "pure" % "0.5.0"
  ),
  testFrameworks += new TestFramework("utest.runner.JvmFramework")
)

lazy val readme = project
  .settings(scalatex.SbtPlugin.projectSettings:_*)
  .dependsOn(api, site)
  .settings(
  libraryDependencies := libraryDependencies.value.filter(!_.toString.contains("scalatex-api")),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "ammonite" % "0.1.4",
    "com.lihaoyi" %% "utest" % "0.2.4"
  ),
  testFrameworks += new TestFramework("utest.runner.JvmFramework"),
  scalaVersion := "2.11.4",
  publish := ()
)

publish := ()
