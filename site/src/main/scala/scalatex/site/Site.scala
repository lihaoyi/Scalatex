package scalatex.site

import java.nio.file.{Paths, Files}

import ammonite.ops.Path
import ammonite.all.{rel => _, _}
import scalatags.Text.all._

/**
 * A semi-abstract trait that encapsulates everything necessary to generate
 * a Scalatex site. Only `content` is left abstract (and needs to be filled
 * in) but the rest of the properties and definitions are all override-able
 * if you wish to customize things.
 */
trait Site{

  def webjars = root/"META-INF"/'resources/'webjars
  def highlightJs = webjars/'highlightjs/"8.2-1"
  def fontAwesome = webjars/"font-awesome"/"4.2.0"
  /**
   * Resources related to the highlight.js library
   */
  def highlightJsResources = Seq(
    highlightJs/"highlight.min.js",
    highlightJs/'styles/"idea.min.css",
    highlightJs/'languages/"scala.min.js",
    highlightJs/'languages/"javascript.min.js",
    highlightJs/'languages/"bash.min.js",
    highlightJs/'languages/"diff.min.js",
    highlightJs/'languages/"xml.min.js"
  )
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
  def autoResources = highlightJsResources ++ pureCss ++ siteCss

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
  def headFrags: Seq[Frag] = Seq(
    link(href:="META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css", rel:="stylesheet"),
    link(href:=stylesName, rel:="stylesheet"),
    script(src:=scriptName),
    script("""
      ['DOMContentLoaded', 'load'].forEach(function(ev){
        addEventListener(ev, function(){
          Array.prototype.forEach.call(
            document.querySelectorAll('code.scalatex-highlight-js'),
            hljs.highlightBlock
          );
        })
      })
    """)
  )

  /**
   * The body of this site's HTML page
   */
  def bodyFrag(frag: Frag): Frag = body(
    maxWidth:="768px",
    marginLeft:="auto",
    marginRight:="auto",
    cls:="scalatex-content",
    frag
  )

  /**
   * The contents of this page
   */
  def content: Map[String, Frag]

  def bundleResources(outputRoot: Path) = {
    for((ext, dest) <- Seq("js" -> scriptName, "css" -> stylesName)) {
      autoResources |? (_.ext == ext) | read.resource |> write.over! outputRoot/dest
    }

    for(res <- manualResources) {
      read.resource.bytes! res |> write.over! outputRoot/(res - root)
    }
  }

  def generateHtml(outputRoot: Path) = {
    for((path, frag) <- content){
      val txt = html(
        head(headFrags),
        bodyFrag(frag)
      ).render

      write.over! outputRoot/path ! txt
    }
  }
  def renderTo(outputRoot: Path) = {
    bundleResources(outputRoot)
    generateHtml(outputRoot)

  }
}

