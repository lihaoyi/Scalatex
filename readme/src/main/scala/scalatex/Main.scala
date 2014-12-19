package scalatex

import java.nio.file.{Paths, Files}

import utest._
import scalatags.Text.all._
object Main {
  def main(args: Array[String]): Unit = {

    val txt = Readme().render
    println(txt)
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
    Files.write(
      Paths.get("readme.html"),
      txt.getBytes
    )
  }
  def exampleRef(start: String, classes: String) = {
    val path = "api/src/test/scala/scalatex/ExampleTests.scala"
    val tq = "\"\"\""
    val classList = classes.split(" ")
    val chunks = for(i <- 0 until classList.length) yield {
      ref(path, Seq("'" + start, tq) ++ Seq.fill(i*2)(tq) ++ Seq(""), Seq(tq), classList(i))(
        width:="50%", float:="left"
      )
    }
    div(chunks, div(clear:="both"))
  }

  def ref(path: String, start: Seq[String], end: Seq[String], className: String) = {
    val txt = io.Source.fromFile(path).getLines().toVector
    var startIndex = 0
    for(str <- start){
      startIndex = txt.indexWhere(_.contains(str), startIndex + 1)
    }
    val endIndex = if (end == Nil) txt.length
    else {
      var endIndex = startIndex
      for (str <- end) {
        endIndex = txt.indexWhere(_.contains(str), endIndex + 1)
      }
      endIndex
    }

    val lines = txt.slice(startIndex, endIndex)
    println(startIndex + "\t" + endIndex)
    val margin = lines.filter(_.trim != "").map(_.takeWhile(_ == ' ').length).min


    pre(code(cls:=className, lines.map(_.drop(margin)).mkString("\n")))
  }
  var depth = 0
  val headers = Seq(h1, h2, h3, h4, h5, h6)
  case class sect(headerString: String) {
    depth += 1
    def apply(body: Frag*) = {
      val res = Seq[Frag](headers(depth - 1)(headerString), body)
      depth -= 1
      res
    }
  }
}
