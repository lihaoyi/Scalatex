package scalatex.site

import java.nio.CharBuffer

import ammonite.ops.{Path, _}

import scalatags.Text.all._
import scalatags.Text.{attrs, tags2}
import scalatex.site.Styles.css
/**
 * A semi-abstract trait that encapsulates everything necessary to generate
 * a Scalatex site. Only `content` is left abstract (and needs to be filled
 * in) but the rest of the properties and definitions are all override-able
 * if you wish to customize things.
 */
trait Site{

  def webjars = root/"META-INF"/'resources/'webjars

  def fontAwesome = webjars/"font-awesome"/"4.2.0"

  /**
   * Resources related to the pure-css library
   */
  def pureCss = Seq(
    webjars/'pure/"0.5.0"/"pure-min.css"
  )
  /**
   * Resources related to the font awesome library
   */
  def fontAwesomeResources = Seq(
    fontAwesome/'fonts/"FontAwesome.otf",
    fontAwesome/'fonts/"fontawesome-webfont.eot",
    fontAwesome/'fonts/"fontawesome-webfont.svg",
    fontAwesome/'fonts/"fontawesome-webfont.ttf",
    fontAwesome/'fonts/"fontawesome-webfont.woff",
    fontAwesome/'css/"font-awesome.min.css"
  )
  /**
   * Resources custom-provided for this particular site
   */
  def siteCss = Set(
    root/'scalatex/'site/"styles.css"
  )

  /**
   * Resources that get automatically included in the bundled js or css file
   */
  def autoResources = pureCss ++ siteCss

  /**
   * Resources copied to the output folder but not included on the page by default
   */
  def manualResources = fontAwesomeResources
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
  def defaultHeader: Seq[Frag] = Seq(
    link(href:="META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css", rel:="stylesheet"),
    link(href:=stylesName, rel:="stylesheet"),
    meta(httpEquiv:="Content-Type", attrs.content:="text/html; charset=UTF-8"),
    tags2.style(raw(css.styleSheetText)),
    script(src:=scriptName)
  )

  /**
   * The body of this site's HTML page
   */
  def bodyFrag(frag: Frag): Frag = div(
    frag
  )


  type Body = Frag
  /** Enable pages to specify multiple header entries */
  type Header = Seq[Frag]
  type Page = (Header, Body)
  /**
   * The contents of the site.
   * Maps String paths to the pages, to their actual content.
   */
  def content: Map[String, Page]

  def bundleResources(outputRoot: Path) = {
    for((ext, dest) <- Seq("js" -> scriptName, "css" -> stylesName)) {
      autoResources |? (_.ext == ext) | read.resource |> write.over! outputRoot/dest
    }

    for(res <- manualResources) {
      read.resource.bytes! res |> write.over! outputRoot/(res relativeTo root)
    }
  }

  def generateHtml(outputRoot: Path) = {
    for((path, (pageHeaders, pageBody)) <- content){
      val txt = html(
        head(pageHeaders),
        body(bodyFrag(pageBody))
      ).render
      val cb = CharBuffer.wrap("<!DOCTYPE html>" + txt)

      val bytes = scala.io.Codec.UTF8.encoder.encode(cb)

      write.over(outputRoot/path, bytes.array())
    }
  }
  def renderTo(outputRoot: Path) = {
    generateHtml(outputRoot)
    bundleResources(outputRoot)
  }
}

