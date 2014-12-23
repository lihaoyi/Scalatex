package scalatex

import java.nio.file.{Paths, Files}
import _root_.scalatex.site.Site
import utest._
import scalatags.Text.all._
object Main {
  def main(args: Array[String]): Unit = {
    def cmp(s1: String, s2: String) = {
      val f1 = s1.filter(!_.isWhitespace).mkString
      val f2 = s2.filter(!_.isWhitespace)
      assert(f1 == f2)
    }
    cmp(
      Hello().render
      ,
      """
        <div>
          Hello World

          <h1>I am a cow!</h1>
        </div>
      """
    )

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
      hl.ref(path, Seq("'" + start, tq) ++ Seq.fill(i*2)(tq) ++ Seq(""), Seq(tq), classList(i))(
        width:="50%", float:="left"
      )
    }
    div(chunks, div(clear:="both"))
  }

  object sect extends site.Section()

  object hl extends site.Highlighter{
    def pathMappings = Seq(
      "" -> "https://github.com/lihaoyi/Scalatex/tree/master"
    )

    def suffixMappings = Map(
      "scalatex" -> "scala",
      "scala" -> "scala"
    )
  }
}
