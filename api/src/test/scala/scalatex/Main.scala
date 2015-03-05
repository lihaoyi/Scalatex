package scalatex

import org.parboiled2.ParseError

import scalatags.Text.all._
object Main {
  def main(args: Array[String]): Unit = {

    println(tw("""
      @p
        hello
      """).render)

    try {
      val res = stages.Parser("""
        @p
          hello
        """, 8)

    } catch{
      case e: ParseError =>
        println(e.position)
        println(e.principalPosition)
        println(e.traces(0))
    }

  }
}//

