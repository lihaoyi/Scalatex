package scalatex.site

import scalatags.Text.all._


trait Highlighter{
  def pathMappings: Map[String, String]
  def suffixMappings: Map[String, String]
  def highlight(string: String, lang: String) = {

    val lines = string.split("\n", -1)
    if (lines.length == 1){
      code(cls:=lang + " highlight-me", lines(0), padding:=0, display:="inline")
    }else{
      val minIndent = lines.map(_.takeWhile(_ == ' ').length)
        .filter(_ > 0)
        .min
      val stripped = lines.map(_.drop(minIndent))
        .dropWhile(_ == "")
        .mkString("\n")

      pre(code(cls:=lang + " highlight-me hljs", stripped))
    }
  }

  def ref(filepath: String,
          start: Seq[String] = Nil,
          end: Seq[String] = Nil,
          className: String = null) = {
    val (startLine, endLine, blob) = {
      val txt = io.Source.fromFile(filepath).getLines().toVector
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
      (startIndex, endIndex, lines.map(_.drop(margin)).mkString("\n"))
    }
    val lang = Option(className)
      .orElse(suffixMappings.get(filepath.split('.').last))
      .getOrElse("")

    val (prefix, url) =
      pathMappings.iterator
                  .find{case (prefix, path) => filepath.startsWith(prefix)}
                  .get

    val hash =
      if (endLine == -1) ""
      else s"#L$startLine-L$endLine"

    val linkUrl =
      s"$url/tree/master/${filepath.drop(prefix.length)}$hash"
    pre(
      code(cls:=lang + " highlight-me hljs", blob),
      a(
        cls:="header-link",
        i(cls:="fa fa-link "),
        position.absolute,
        right:="0.5em",
        bottom:="0.5em",
        display.block,
        fontSize:="24px",
        href:=linkUrl,
        target:="_blank"
      )
    )
  }
}