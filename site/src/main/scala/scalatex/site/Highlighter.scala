package scalatex.site
import collection.mutable
import ammonite.ops.{RelPath, Path}

import scalatags.Text.all._
import ammonite.ops._
import Styles.css
/**
 * Lets you instantiate a Highlighter object. This can be used to reference
 * snippets of code from files within your project via the `.ref` method, often
 * used via `hl.ref` where `hl` is a previously-instantiated Highlighter.
 */
trait Highlighter{
  val languages = mutable.Set.empty[String]
  def webjars = root/"META-INF"/'resources/'webjars
  def highlightJs = webjars/'highlightjs/"8.2-1"
  def style: String = "idea"

  def lang(name: String) = (s: String) => this.highlight(s, name)
  def as = lang("actionscript")
  def scala = lang("scala")
  def asciidoc = lang("asciidoc")
  def ahk = lang("autohotkey")
  def sh = lang("bash")
  def clj = lang("clojure")
  def coffee = lang("coffeescript")
  def ex = lang("elixir")
  def erl = lang("erlang")
  def fs = lang("fsharp")
  def hs = lang("haskell")
  def hx = lang("haxe")
  def js = lang("javascript")
  def nim = lang("nimrod")
  def rb = lang("ruby")
  def ts = lang("typescript")
  def vb = lang("vbnet")
  def xml = lang("xml")
  def diff = lang("diff")
  def autoResources = {
    Seq(highlightJs/"highlight.min.js") ++
    Seq(highlightJs/'styles/s"$style.min.css") ++
    languages.map(x => highlightJs/'languages/s"$x.min.js")
  }
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
  def pathMappings: Seq[(Path, String)] = Nil

  /**
   * A mapping of file name suffixes to highlight.js classes.
   * Usually something like:
   *
   * Map(
   *   "scala" -> "scala",
   *   "js" -> "javascript"
   * )
   */
  def suffixMappings: Map[String, String] = Map(
    "scala" -> "scala",
    "sbt" -> "scala",
    "scalatex" -> "scala",
    "as" -> "actionscript",
    "ahk" -> "autohotkey",
    "coffee" -> "coffeescript",
    "clj" -> "clojure",
    "cljs" -> "clojure",
    "sh" -> "bash",
    "ex" -> "elixir",
    "erl" -> "erlang",
    "fs" -> "fsharp",
    "hs" -> "haskell",
    "hx" -> "haxe",
    "js" -> "javascript",
    "nim" -> "nimrod",
    "rkt" -> "lisp",
    "scm" -> "lisp",
    "sch" -> "lisp",
    "rb" -> "ruby",
    "ts" -> "typescript",
    "vb" -> "vbnet"
  )

  /**
   * Highlight a short code snippet with the specified language
   */
  def highlight(string: String, lang: String) = {
    languages.add(lang)
    val lines = string.split("\n", -1)
    if (lines.length == 1){
      code(
        cls:=lang + " " + Styles.css.highlightMe.name,
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

      pre(code(cls:=lang + " " + Styles.css.highlightMe.name, stripped))
    }
  }
  import Highlighter._
  /**
   * Grab a snippet of code from the given filepath, and highlight it.
   *
   * @param filePath The file containing the code in question
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
         (filePath: ammonite.ops.BasePath[_],
          start: S = Nil,
          end: V = Nil,
          className: String = null) = {
    val absPath = filePath match{
      case p: Path => p
      case p: RelPath => cwd/p
    }

    val ext = filePath.last.split('.').last
    val lang = Option(className)
      .getOrElse(suffixMappings.getOrElse(ext, ext))


    val linkData =
      pathMappings.iterator
                  .find{case (prefix, path) => absPath > prefix}
    val (startLine, endLine, blob) = referenceText(absPath, start, end)
    val link = linkData.map{ case (prefix, url) =>
      val hash =
        if (endLine == -1) ""
        else s"#L$startLine-L$endLine"

      val linkUrl = s"$url/${absPath - prefix}$hash"
      a(
        css.headerLink,
        i(cls:="fa fa-link "),
        position.absolute,
        right:="0.5em",
        top:="0.5em",
        display.block,
        fontSize:="24px",
        href:=linkUrl,
        target:="_blank"
      )
    }

    pre(
      css.hoverContainer,
      code(cls:=lang + " " + Styles.css.highlightMe.name, blob),
      link
    )
  }

  def referenceText[S: RefPath, V: RefPath](filepath: Path, start: S, end: V) = {
    val txt = read.lines! filepath
    // Start from -1 so that searching for things on the first line of the file (-1 + 1 = 0)
    var startIndex = -1
    for(str <- implicitly[RefPath[S]].apply(start)){
      startIndex = txt.indexWhere(_.contains(str), startIndex + 1)
    }
    // But if there are no selectors, start from 0 and not -1
    startIndex = startIndex max 0

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
  def snippet = script(raw(s"""
    ['DOMContentLoaded', 'load'].forEach(function(ev){
      addEventListener(ev, function(){
        Array.prototype.forEach.call(
          document.getElementsByClassName('${Styles.css.highlightMe.name}'),
          hljs.highlightBlock
        );
      })
    })
  """))
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
