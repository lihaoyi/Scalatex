package scalatex
import scalatags.Text.all._
object Main {
  def main(args: Array[String]): Unit = {
    println(scalatex.tw(
    """@div
      @if(true)
        Hello
      @else
        lols
    """
    ).render)
  }
}
