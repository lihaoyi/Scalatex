package scalatex

import sbt.Keys._
import sbt._
object SbtPlugin extends sbt.AutoPlugin{
  val scalatexVersion = "0.1.6"
  val scalatexDirectory = taskKey[sbt.File]("Clone stuff from github")
  val mySeq = Seq(
    scalatexDirectory := sourceDirectory.value / "scalatex",
    managedSources ++= {
      val inputDir = scalatexDirectory.value
      val outputDir = sourceManaged.value / "scalatex"
      val inputFiles = (inputDir ** "*.scalatex").get
      println("Generating Scalatex Sources...")
      val outputFiles = for(inFile <- inputFiles) yield {
        val outFile = new sbt.File(
          outputDir.getAbsolutePath + inFile.getAbsolutePath.drop(inputDir.getAbsolutePath.length).dropRight(3)
        )
        val name = inFile.getName
        val objectName = name.slice(name.lastIndexOf('/')+1, name.lastIndexOf('.'))
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
            |$pkgName
            |import scalatags.Text.all._
            |
            |object $objectName{
            |  def apply(): Frag = scalatex.twf("${inFile.getAbsolutePath}")
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
      "com.lihaoyi" %% "scalatex-api" % scalatexVersion,
      "com.scalatags" %% "scalatags" % "0.4.2"
    ),
    watchSources ++= ((scalatexDirectory in Compile).value ** "*.scalatex").get
  )
}

object ScalatexReadme{
  def apply(folder: String = "scalatex", 
            source: String,
            url: String,
            targetFolder: String,
            autoResources: Seq[String] = Nil) = Project(
    id = folder,
    base = file(folder),
    settings = scalatex.SbtPlugin.projectSettings ++ Seq(
      sourceGenerators in Compile <+= sourceManaged in Compile map { dir =>
        val file = dir / "scalatex" / "Main.scala"
        IO.write(file, s"""
          object Main extends scalatex.site.Main(
            url = "$url",
            wd = ammonite.ops.cwd,
            output = ammonite.ops.cwd/ammonite.ops.RelPath("$targetFolder"),
            extraAutoResources = Seq(${autoResources.map('"' + _ + '"').mkString(",")}).map(ammonite.ops.root/ammonite.ops.RelPath(_)),
            $source()
          )
        """)
        Seq(file)
      },
      (SbtPlugin.scalatexDirectory in Compile) := file(folder),
      libraryDependencies += "com.lihaoyi" %% "scalatex-site" % SbtPlugin.scalatexVersion,
      scalaVersion := "2.11.6"
    )
  )
   .enablePlugins(SbtPlugin)

}
