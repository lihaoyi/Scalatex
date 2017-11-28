package scalatex

import java.nio.file.Paths

import sbt.Keys._
import sbt._

import PluginCompat._

object SbtPlugin extends sbt.AutoPlugin {
  val scalatexVersion = scalatex.Constants.version
  val scalatexDirectory = taskKey[sbt.File]("Clone stuff from github")
  val mySeq = Seq(
    scalatexDirectory := sourceDirectory.value / "scalatex",
    managedSources ++= {
      val inputDir = scalatexDirectory.value
      val outputDir = sourceManaged.value / "scalatex"
      val inputFiles = (inputDir ** "*.scalatex").get
      println("Generating Scalatex Sources...")
      val outputFiles = for (inFile <- inputFiles) yield {
        val relativeInputFile = inputDir.toPath.relativize(inFile.toPath)
        val outFile = new sbt.File(
          outputDir.getAbsolutePath + inFile.getAbsolutePath.drop(inputDir.getAbsolutePath.length).dropRight(3)
        )
        val name = inFile.getName
        val objectName = name.slice(name.lastIndexOf('/') + 1, name.lastIndexOf('.'))
        val pkgName =
          fixPath(inFile)
            .drop(inputDir.getAbsolutePath.length + 1)
            .toString
            .split("/")
            .dropRight(1)
            .map(s => s"package $s")
            .mkString("\n")
        IO.write(
          outFile,
          s"""
            |package scalatex
            |$pkgName
            |import scalatags.Text.all._
            |
            |object $objectName{
            |  def apply(): Frag = _root_.scalatex.twf("${fixPath(inFile)}")
            |  def sourcePath = "$relativeInputFile"
            |}
            |
            |${IO.readLines(inFile).map("//"+_).mkString("\n")}
          """.stripMargin
        )
        outFile
      }
      outputFiles
    }
  )
  override val projectSettings = inConfig(Test)(mySeq) ++ inConfig(Compile)(mySeq) ++ Seq(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "scalatex-api" % scalatexVersion
    ),
    watchSources ++= {
      val compileTarget = (target in Compile).value
      for{
        f <- (scalatexDirectory in Compile).value.get
        if f.relativeTo(compileTarget).isEmpty
      } yield f
    }
  )
  // fix backslash path separator
  def fixPath(file:String) = file.replaceAll("\\\\","/")
  def fixPath(file:sbt.File):String = fixPath(file.getAbsolutePath)
}
object ScalatexReadme{
  import SbtPlugin.fixPath
  /**
   *
   * @param projectId The name this readme project will take,
   *                  and the folder it will live in
   * @param wd The working directory of this readme project will
   *           use as `wd` inside the code
   * @param source The name of the scalatex file which will be bundled into
   *               `index.html`, without the `.scalatex` suffix
   * @param url Where this project lives on the internet, so `hl.ref` can
   *            properly insert links
   * @param autoResources Any other CSS or JS files you want to bundle into
   *                      index.html
   */
  def apply(projectId: String = "scalatex",
            wd: java.io.File,
            source: String,
            url: String,
            autoResources: Seq[String] = Nil) =
    Project(id = projectId, base = file(projectId)).settings(
      resourceDirectory in Compile := file(projectId) / "resources",
      sourceGenerators in Compile += task{
        val dir = (sourceManaged in Compile).value
        val manualResources: Seq[String] = for{
          f <- (file(projectId) / "resources" ** "*").get
          if f.isFile
          rel <- f.relativeTo(file(projectId) / "resources")
        } yield fixPath(rel.getPath)

        val generated = dir / "scalatex" / "Main.scala"

        val autoResourcesStrings = autoResources.map('"' + _ + '"').mkString(",")

        val manualResourceStrings = manualResources.map('"' + _ + '"').mkString(",")
        IO.write(generated, s"""
          package scalatex
          object Main extends scalatex.site.Main(
            url = "$url",
            wd = ammonite.ops.Path("${fixPath(wd)}"),
            output = ammonite.ops.Path("${fixPath((target in Compile).value / "scalatex")}"),
            extraAutoResources = Seq[String]($autoResourcesStrings).map(ammonite.ops.resource/ammonite.ops.RelPath(_)),
            extraManualResources = Seq[String]($manualResourceStrings).map(ammonite.ops.resource/ammonite.ops.RelPath(_)),
            scalatex.$source()
          )
        """)
        Seq(generated)
      },
      (SbtPlugin.scalatexDirectory in Compile) := file(projectId),
      libraryDependencies += "com.lihaoyi" %% "scalatex-site" % SbtPlugin.scalatexVersion,
      scalaVersion := _root_.scalatex.Constants.scala212
    ).enablePlugins(SbtPlugin)
}
