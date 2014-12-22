package scalatex.site

import java.nio.file.{Paths, Files}

import scala.collection.mutable
import scalatags.Text.all
import scalatags.Text.all._

/**
 * A semi-abstract trait that encapsulates everything necessary to generate
 * a Scalatex site. Only `content` is left abstract (and needs to be filled
 * in) but the rest of the properties and definitions are all override-able
 * if you wish to customize things.
 */
trait Site{
  /**
   * Resources related to the highlight.js library
   */
  def highlightJsResources = Set(
    "META-INF/resources/webjars/highlightjs/8.2-1/highlight.min.js",
    "META-INF/resources/webjars/highlightjs/8.2-1/styles/idea.min.css",
    "META-INF/resources/webjars/highlightjs/8.2-1/languages/scala.min.js",
    "META-INF/resources/webjars/highlightjs/8.2-1/languages/javascript.min.js",
    "META-INF/resources/webjars/highlightjs/8.2-1/languages/bash.min.js",
    "META-INF/resources/webjars/highlightjs/8.2-1/languages/diff.min.js",
    "META-INF/resources/webjars/highlightjs/8.2-1/languages/xml.min.js"
  )
  /**
   * Resources related to the pure-css library
   */
  def pureCss = Set(
    "META-INF/resources/webjars/pure/0.5.0/pure-min.css"
  )
  /**
   * Resources related to the font awesome library
   */
  def fontAwesome = Set(
    "META-INF/resources/webjars/font-awesome/4.2.0/fonts/FontAwesome.otf",
    "META-INF/resources/webjars/font-awesome/4.2.0/fonts/fontawesome-webfont.eot",
    "META-INF/resources/webjars/font-awesome/4.2.0/fonts/fontawesome-webfont.svg",
    "META-INF/resources/webjars/font-awesome/4.2.0/fonts/fontawesome-webfont.ttf",
    "META-INF/resources/webjars/font-awesome/4.2.0/fonts/fontawesome-webfont.woff",
    "META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css"
  )
  /**
   * Resources custom-provided for this particular site
   */
  def siteCss = Set(
    "scalatex/site/styles.css"
  )

  /**
   * Resources that get automatically included in the bundled js or css file
   */
  def autoResources = highlightJsResources | pureCss | siteCss

  /**
   * Resources copied to the output folder but not included on the page by default
   */
  def manualResources = fontAwesome
  /**
   * The name of the javascript file that all javascript resources get bundled into
   */
  def scriptName = "scripts.js"
  /**
   * The name of the css file that all css resources get bundled into
   */
  def stylesName = "styles.css"

  /**
   * The header of this site's HTML page
   */
  def headFrag = head(
    link(href:="META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css", rel:="stylesheet"),
    link(href:=stylesName, rel:="stylesheet"),
    script(src:=scriptName),
    script("hljs.initHighlightingOnLoad();")
  )

  /**
   * The body of this site's HTML page
   */
  def bodyFrag = body(maxWidth:="768px", marginLeft:="auto", marginRight:="auto")(
    content
  )

  /**
   * The contents of this page
   */
  def content: Frag
  def bundleResources(outputRoot: String) = {
    val jsFiles = autoResources.filter(_.endsWith(".js")).toSet
    val cssFiles = autoResources.filter(_.endsWith(".css")).toSet
    for((resources, dest) <- Seq(jsFiles -> scriptName, cssFiles -> stylesName)) {
      val blobs = for(res <- resources.iterator) yield {
        io.Source.fromInputStream(getClass.getResourceAsStream("/"+res)).mkString
      }

      Files.write(
        Paths.get(outputRoot + dest),
        blobs.mkString("\n").getBytes
      )
    }
    for(res <- manualResources) {
      val dest = outputRoot + res
      Paths.get(dest).toFile.getParentFile.mkdirs()
      Files.deleteIfExists(Paths.get(dest))
      Files.copy(getClass.getResourceAsStream("/" + res), Paths.get(dest))
    }
  }
  def generateHtml(outputRoot: String) = {
    val txt = html(
      headFrag,
      bodyFrag
    ).render
    Files.write(
      Paths.get(outputRoot + "readme.html"),
      txt.getBytes
    )
  }
  def renderTo(outputRoot: String) = {
    new java.io.File(outputRoot).mkdirs()
    bundleResources(outputRoot)
    generateHtml(outputRoot)

  }
}

trait HeaderStrategy{
  def header(name: String, subname: String, anchor: Frag): ConcreteHtmlTag[String]
  def content(frag: Frag): Frag
}
object HeaderStrategy{
  def apply(h: (String, String, Frag) => ConcreteHtmlTag[String], c: Frag => Frag = f => f) = {
    new HeaderStrategy {
      def header(name: String, subname: String, anchor: Frag) = h(name, subname, anchor)
      def content(frag: all.Frag) = c(frag)
    }
  }
  implicit def TagToHeaderStrategy(t: ConcreteHtmlTag[String]): HeaderStrategy =
    HeaderStrategy((name, subname, frag) => t(name, frag))
}

case class Tree[T](value: T, children: mutable.Buffer[Tree[T]])

class Section{
  var structure = Tree[String]("root", mutable.Buffer.empty)
  var depth = 0
  val headers: Seq[HeaderStrategy] = Seq(h1, h2, h3, h4, h5, h6)
  def munge(name: String): String = name.replace(" ", "")
  def headingAnchor(name: String) = a(
    cls:="header-link",
    href:=s"#${munge(name)}",
    " ",
    i(cls:="fa fa-link")
  )
  def apply(header: String, subHeader: String = "") = {
    depth += 1
    val newNode = Tree[String](header, mutable.Buffer.empty)
    structure.children.append(newNode)
    val prev = structure
    structure = newNode
    new SectionProxy(body => {
      val hs = headers(depth - 1)
      val munged = munge(header)
      val res = Seq[Frag](
        hs.header(header, subHeader, headingAnchor(munged))(id:=munged),
        hs.content(body)
      )
      depth -= 1
      structure = prev
      res
    })
  }
}
class SectionProxy(func: Seq[Frag] => Frag){
  def apply(body: Frag*) = func(body)
}
