lazy val Constants = _root_.scalatex.Constants
sharedSettings
noPublish

version in ThisBuild := Constants.version
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseVersionBump := sbtrelease.Version.Bump.Minor
releaseTagComment    := s"Releasing ${(version in ThisBuild).value}"
releaseCommitMessage := s"Bump version to ${(version in ThisBuild).value}"
sonatypeProfileName := "lihaoyi"
releaseCrossBuild := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              
  //inquireVersions,                        
  runClean,                           
  runTest,                            
  //setReleaseVersion,                      
  //commitReleaseVersion,                 
  tagRelease,                             
  publishArtifacts,
  //setNextVersion,                         
  //commitNextVersion,                  
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges               
)

def supportedScalaVersion = Seq(Constants.scala212, Constants.scala213)

lazy val sharedSettings = Seq(
  version := Constants.version,
  organization := "com.lihaoyi",
  scalaVersion := Constants.scala213,
  crossScalaVersions := supportedScalaVersion,
  libraryDependencies += "com.lihaoyi" %% "acyclic" % "0.2.0" % "provided",
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.2.0"),
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

lazy val circe =
  Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % Constants.circe)

lazy val api = project.settings(sharedSettings:_*)
  .settings(
    name := "scalatex-api",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "utest" % Constants.uTest % "test",
      "com.lihaoyi" %% "scalaparse" % "2.2.3",
      "com.lihaoyi" %% "scalatags" % Constants.scalaTags,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalatexSbtPlugin = project.settings(sharedSettings:_*)
  .settings(
  name := "scalatex-sbt-plugin",
  scalaVersion := Constants.scala212,
  (skip in Compile) := {
    scalaBinaryVersion.value match {
      case "2.12" => false
      case _ => true
    }
  },
  crossSbtVersions := List("1.3.7"),
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
  crossScalaVersions := supportedScalaVersion,
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "utest" % Constants.uTest % "test",
    "com.lihaoyi" %% "ammonite-ops" % "2.0.4",
    "org.webjars.bower" % "highlightjs" % "9.12.0",
    "org.webjars.bowergithub.highlightjs" % "highlight.js" % "9.12.0", 
    "org.webjars" % "font-awesome" % "4.7.0",
    "com.lihaoyi" %% "scalatags" % Constants.scalaTags,
    "org.webjars" % "pure" % "0.6.2",
    "org.scalaj" %% "scalaj-http" % "2.4.2"
  ) ++ circe,
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
    scalaVersion := Constants.scala213,
    crossScalaVersions := supportedScalaVersion,
    scalacOptions += "-P:scalajs:suppressExportDeprecations", // see https://github.com/scala-js/scala-js/issues/3092
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.8",
      "com.lihaoyi" %%% "scalatags" % Constants.scalaTags,
      "io.circe" %%% "circe-core" % Constants.circe,
      "io.circe" %%% "circe-generic" % Constants.circe,
      "io.circe" %%% "circe-parser" % Constants.circe
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
