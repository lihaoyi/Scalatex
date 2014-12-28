package scalatex.site

import scalatags.Text.all._

/**
 * Lets you instantiate a Highlighter object. This can be used to reference
 * snippets of code from files within your project via the `.ref` method, often
 * used via `hl.ref` where `hl` is a previously-instantiated Highlighter.
 */
trait Highlighter{
  /**
   * A mapping of file-path-prefixes to URLs where the source
   * can be accessed. e.g.
   *
   * Seq(
   *   "clones/scala-js" -> "https://github.com/scala-js/scala-js/blob/master",
   *   "" -> "https://github.com/lihaoyi/scalatex/blob/master"
   * )
   *
   * Will link any code reference from clones/scala-js to the scala-js
   * github repo, while all other paths will default to the scalatex
   * github repo.
   *
   * If a path is not covered by any of these rules, no link is rendered
   */
  def pathMappings: Seq[(String, String)] = Nil

  /**
   * A mapping of file name suffixes to highlight.js classes.
   * Usually something like:
   *
   * Map(
   *   "scala" -> "scala",
   *   "js" -> "javascript"
   * )
   */
  def suffixMappings: Map[String, String]

  /**
   * Highlight a short code snippet with the specified language
   */
  def highlight(string: String, lang: String) = {

    val lines = string.split("\n", -1)
    if (lines.length == 1){
      code(
        cls:=lang + " highlight-me",
        display:="inline",
        padding:=0,
        margin:=0,
        lines(0)
      )

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
  import Highlighter._
  /**
   * Grab a snippet of code from the given filepath, and highlight it.
   *
   * @param filepath The file containing the code in question
   * @param start Snippets used to navigate to the start of the snippet
   *              you want, from the beginning of the file
   * @param end Snippets used to navigate to the end of the snippet
   *            you want, from the start of start of the snippet
   * @param className An optional css class set on the rendered snippet
   *                  to determine what language it gets highlighted as.
   *                  If not given, it defaults to the class given in
   *                  [[suffixMappings]]
   */
  def ref[S: RefPath, V: RefPath]
         (filepath: String,
          start: S = Nil,
          end: V = Nil,
          className: String = null) = {

    val lang = Option(className)
      .orElse(suffixMappings.get(filepath.split('.').last))
      .getOrElse("")

    val linkData =
      pathMappings.iterator
                  .find{case (prefix, path) => filepath.startsWith(prefix)}
    val (startLine, endLine, blob) = referenceText(filepath, start, end)
    val link = linkData.map{ case (prefix, url) =>
      val hash =
        if (endLine == -1) ""
        else s"#L$startLine-L$endLine"

      val linkUrl = s"$url/${filepath.drop(prefix.length)}$hash"
      a(
        cls:="scalatex-header-link",
        i(cls:="fa fa-link "),
        position.absolute,
        right:="0.5em",
        bottom:="0.5em",
        display.block,
        fontSize:="24px",
        href:=linkUrl,
        target:="_blank"
      )
    }

    pre(
      cls:="scalatex-header-container",
      code(cls:=lang + " scalatex-highlight-js", blob),
      link
    )
  }
  def referenceText[S: RefPath, V: RefPath](filepath: String, start: S, end: V) = {
    val txt = io.Source.fromFile(filepath).getLines().toVector
    var startIndex = 0

    for(str <- implicitly[RefPath[S]].apply(start)){
      startIndex = txt.indexWhere(_.contains(str), startIndex + 1)
    }
    val startIndent = txt(startIndex).takeWhile(_.isWhitespace).length
    val endQuery = implicitly[RefPath[V]].apply(end)
    val endIndex = if (endQuery == Nil) {
      val next = txt.drop(startIndex).takeWhile{ line =>
        line.trim == "" || line.takeWhile(_.isWhitespace).length >= startIndent
      }
      startIndex + next.length
    } else {
      var endIndex = startIndex
      for (str <- endQuery) {
        endIndex = txt.indexWhere(_.contains(str), endIndex + 1)
      }

      endIndex
    }

    val margin = txt(startIndex).takeWhile(_.isWhitespace).length
    val lines = txt.slice(startIndex, endIndex)
                   .map(_.drop(margin))
                   .reverse
                   .dropWhile(_.trim == "")
                   .reverse

    (startIndex, endIndex, lines.mkString("\n"))

  }
}
object Highlighter{

  /**
   * A context bound used to ensure you pass a `String`
   * or `Seq[String]` to the `@hl.ref` function
   */
  trait RefPath[T]{
    def apply(t: T): Seq[String]
  }
  object RefPath{
    implicit object StringRefPath extends RefPath[String]{
      def apply(t: String) = Seq(t)
    }
    implicit object SeqRefPath extends RefPath[Seq[String]]{
      def apply(t: Seq[String]) = t
    }
    implicit object NilRefPath extends RefPath[Nil.type]{
      def apply(t: Nil.type) = t
    }
  }

}
