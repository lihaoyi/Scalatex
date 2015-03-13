package scalatex

import java.nio.file.{Paths, Files}
import site.Site
import scalatags.Text.all._

import ammonite.ops._

object Main {
  val wd = ammonite.ops.cwd
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
      override def autoResources = super.autoResources ++ Seq(root/'scalatex/'scrollspy/"scrollspy.js")
      override def bodyFrag(frag: Frag) = {
        Seq(
          ghLink,
          super.bodyFrag(Seq(
            div(
              position.fixed,
              overflow.scroll,
              backgroundColor := "#191818",
              height := "100%",
              width := 250,
              left := 0,
              a(href:="#menu", id:="menu-link", cls:="menu-link")(
                span
              ),
              div(id:="menu")
            ),
            frag
          )),
          script(raw(s"""
            scalatex.scrollspy.Controller().main(
              ${upickle.write(sect.structure.children(0))},
              document.getElementById("menu"),
              document.getElementById("menu-link")
            )"""))
        )
      }
      def content = Map(
        "index.html" -> scalatex.Readme()
      )
    }.renderTo(wd/'target/'site)
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
    val path = wd/'api/'src/'test/'scala/'scalatex/"ExampleTests.scala"
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
    Main.wd -> "https://github.com/lihaoyi/Scalatex/tree/master"
  )

  def suffixMappings = Map(
    "scalatex" -> "scala",
    "scala" -> "scala"
  )
  def scala(s: String) = this.highlight(s, "scala")
}
