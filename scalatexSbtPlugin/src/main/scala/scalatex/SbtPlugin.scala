package scalatex

import sbt.Keys._
import sbt._
object SbtPlugin extends sbt.AutoPlugin {
  val scalatexVersion = "0.2.2-SNAPSHOT"
  val scalatexDirectory = taskKey[sbt.File]("Clone stuff from github")
  val mySeq = Seq(
    scalatexDirectory := sourceDirectory.value / "scalatex",
    managedSources ++= {
      val inputDir = scalatexDirectory.value
      val outputDir = sourceManaged.value / "scalatex"
      val inputFiles = (inputDir ** "*.scalatex").get
      println("Generating Scalatex Sources...")
      val outputFiles = for (inFile <- inputFiles) yield {
        val outFile = new sbt.File(
          outputDir.getAbsolutePath + inFile.getAbsolutePath.drop(inputDir.getAbsolutePath.length).dropRight(3)
        )
        val name = inFile.getName
        val objectName = name.slice(name.lastIndexOf('/') + 1, name.lastIndexOf('.'))
        val pkgName =
          inFile.getAbsolutePath
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
            |  def apply(): Frag = _root_.scalatex.twf("${inFile.getAbsolutePath}")
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
      "com.lihaoyi" %% "scalatex-api" % scalatexVersion, "com.scalatags" %% "scalatags" % "0.4.2"
    ),
    watchSources ++= {
      for{
        f <- (scalatexDirectory in Compile).value.get
        if f.relativeTo((target in Compile).value).isEmpty
      } yield f
    }
  )
}
object ScalatexReadme{
  def apply(projectId: String = "scalatex",
            wd: java.io.File,
            source: String,
            url: String,
            autoResources: Seq[String] = Nil) = Project(
    id = projectId,
    base = file(projectId),
    settings = scalatex.SbtPlugin.projectSettings ++ Seq(
      resourceDirectory in Compile := file(projectId) / "resources",
      sourceGenerators in Compile += task{
        val dir = (sourceManaged in Compile).value
        val manualResources: Seq[String] = for{
          f <- (file(projectId) / "resources" ** "*").get
          if f.isFile
          rel <- f.relativeTo(file(projectId) / "resources")
        } yield rel.getPath

        val generated = dir / "scalatex" / "Main.scala"

        IO.write(generated, s"""
          package scalatex
          object Main extends scalatex.site.Main(
            url = "$url",
            wd = ammonite.ops.Path("${wd.getAbsolutePath}"),
            output = ammonite.ops.Path("${((target in Compile).value / "scalatex").getAbsolutePath}"),
            extraAutoResources = Seq[String](${autoResources.map('"' + file(_).getCanonicalPath + '"').mkString(",")}).map(ammonite.ops.root/ammonite.ops.RelPath(_)),
            extraManualResources = Seq[String](${manualResources.map('"' + _ + '"').mkString(",")}).map(ammonite.ops.root/ammonite.ops.RelPath(_)),
            scalatex.$source()
          )
        """)
        Seq(generated)
      },
      (SbtPlugin.scalatexDirectory in Compile) := file(projectId),
      libraryDependencies += "com.lihaoyi" %% "scalatex-site" % SbtPlugin.scalatexVersion,
      scalaVersion := "2.11.6"
    )
  ).enablePlugins(SbtPlugin)
}
