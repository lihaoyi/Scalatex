package scalatex

import java.nio.file.{Paths, Files}
import _root_.scalatex.site.Site
import utest._
import scalatags.Text.all._
import scalatags.generic
import scalatags.text.Builder

object Main {
  def main(args: Array[String]): Unit = {
    val ghLink = a(
      href:="https://github.com/lihaoyi/scalatex",
      position.absolute,
      top:=0,right:=0,border:=0,
      img(
        src:="https://camo.githubusercontent.com/a6677b08c955af8400f44c6298f40e7d19cc5b2d/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677261795f3664366436642e706e67",
        alt:="Fork me on GitHub"
      )
    )

    new Site{
      override def bodyFrag(frag: Frag) = {
        Seq(ghLink, super.bodyFrag(frag))
      }
      def content = Map(
        "readme.html" -> scalatex.Readme()
      )
    }.renderTo("target/site/")
  }
  def exampleWrapper(f: Frag*) = Seq(
    hr,
    div(
      opacity:="0.6",
      fontStyle.oblique,
      f
    ),
    hr
  )
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
  def late(frags: => Frag) = new Late(() => frags)
  class Late(frags: () => Frag) extends scalatags.text.Frag{
    def render: String = frags().render
    def writeTo(strb: StringBuilder): Unit = strb.append(render)
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
