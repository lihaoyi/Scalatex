package scalatex

import sbt.Keys._
import sbt._
object SbtPlugin extends sbt.AutoPlugin {
  val scalatexVersion = scalatex.Constants.version
  val scalatexDirectory = settingKey[sbt.File]("Clone stuff from github")
  val scalatexGenerateMain = settingKey[Boolean]("Generate main function for scalatex site.")
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
      for{
        f <- (scalatexDirectory in Compile).value.get
        if f.relativeTo((target in Compile).value).isEmpty
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
            autoResources: Seq[String] = Nil) = Project(
    id = projectId,
    base = file(projectId),
    settings = scalatex.SbtPlugin.projectSettings ++ Seq(
      resourceDirectory in Compile := file(projectId) / "resources",
      SbtPlugin.scalatexGenerateMain := true,
      sourceGenerators in Compile += task{
        val generateMain = SbtPlugin.scalatexGenerateMain.value
        val dir = (sourceManaged in Compile).value
        val manualResources: Seq[String] = for{
          f <- (file(projectId) / "resources" ** "*").get
          if f.isFile
          rel <- f.relativeTo(file(projectId) / "resources")
        } yield fixPath(rel.getPath)


        val scalatexDirectory = SbtPlugin.scalatexDirectory.in(Compile).value
        val scalatexFiles = for {
          f <- (scalatexDirectory ** "*.scalatex").get
          if f.isFile
          rel <- f.relativeTo(scalatexDirectory)
          string = fixPath(rel.getPath)
        } yield s"""    "$string" -> ${string.stripSuffix(".scalatex").replace('/', '.')}()"""
        val scalatexFilesStrings = scalatexFiles.mkString("\n", ",\n", "  \n")

        val generated = dir / "scalatex" / "Main.scala"

        val autoResourcesStrings = autoResources.map('"' + _ + '"').mkString(",")
        val mainObject =
          if (generateMain)
            """|
               |object Main extends scalatex.site.Main(
               |  url = MainInfo.url,
               |  wd = MainInfo.wd,
               |  output = MainInfo.output,
               |  extraAutoResources = MainInfo.extraAutoResources,
               |  extraManualResources = MainInfo.extraManualResources,
               |  frag = MainInfo.index
               |)
               |""".stripMargin
          else ""

        val manualResourceStrings = manualResources.map('"' + _ + '"').mkString(",")
        IO.write(generated, s"""package scalatex
          |
          |trait MainInfo extends scalatex.site.SiteInfo {
          |  def url = "$url"
          |  def wd = ammonite.ops.Path("${fixPath(wd)}")
          |  def output = ammonite.ops.Path("${fixPath((target in Compile).value / "scalatex")}")
          |  def extraAutoResources = Seq[String]($autoResourcesStrings).map(ammonite.ops.resource/ammonite.ops.RelPath(_))
          |  def extraManualResources = Seq[String]($manualResourceStrings).map(ammonite.ops.resource/ammonite.ops.RelPath(_))
          |  def scalatexFilesRoot = ammonite.ops.Path("${scalatexDirectory.getAbsolutePath}")
          |  def scalatexFiles: Map[String, scalatags.Text.all.Frag] = Map($scalatexFilesStrings)
          |  def index = scalatexFiles("$source.scalatex")
          |}
          |object MainInfo extends MainInfo
          |$mainObject""".stripMargin)
        Seq(generated)
      },
      (SbtPlugin.scalatexDirectory in Compile) := file(projectId),
      libraryDependencies += "com.lihaoyi" %% "scalatex-site" % SbtPlugin.scalatexVersion,
      scalaVersion := "2.12.1"
    )
  ).enablePlugins(SbtPlugin)
}
