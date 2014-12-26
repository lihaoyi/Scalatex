package scalatex

import java.nio.file.{Paths, Files}
import _root_.scalatex.site.Site
import utest._
import scalatags.Text.all._
object Main {
  def main(args: Array[String]): Unit = {

    new Site{
      def content = Map(
        "readme.html" -> scalatex.Readme()
      )
    }.renderTo("target/site/")
  }
  def exampleRef(start: String, classes: String) = {
    val path = "api/src/test/scala/scalatex/ExampleTests.scala"
    val tq = "\"\"\""
    val classList = classes.split(" ")
    val chunks = for(i <- 0 until classList.length) yield {
      hl.ref(path, Seq("'" + start, tq) ++ Seq.fill(i*2)(tq) ++ Seq(""), Seq(tq), classList(i)).apply(
        width:="50%", float:="left"
      )
    }
    div(chunks, div(clear:="both"))
  }
}

object sect extends site.Section()

object hl extends site.Highlighter{
  override def pathMappings = Seq(
    "" -> "https://github.com/lihaoyi/Scalatex/tree/master"
  )

  def suffixMappings = Map(
    "scalatex" -> "scala",
    "scala" -> "scala"
  )
  def scala(s: String) = this.highlight(s, "scala")
}
