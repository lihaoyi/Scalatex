package scalatex

import scalatags.Text.all._

package object site {
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
}
