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
  def exampleWrapper(f: Frag*) = Seq(hr, div(opacity:="0.6", fontStyle.oblique)(f), hr)
  def pairs(frags: Frag*) = div(frags, div(clear:="both"))
  def half(frags: Frag*) = div(frags, width:="50%", float.left)
  def exampleRef(start: String, classes: String) = {
    val path = "api/src/test/scala/scalatex/ExampleTests.scala"
    val tq = "\"\"\""
    val classList = classes.split(" ")
    val chunks = for(i <- 0 until classList.length) yield half{
      hl.ref(path, Seq("'" + start, tq) ++ Seq.fill(i*2)(tq) ++ Seq(""), Seq(tq), classList(i))
    }
    pairs(chunks)
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
