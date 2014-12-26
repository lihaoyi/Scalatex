package scalatex.site

import java.nio.file.{Paths, Files}

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
  def headFrags = Seq(
    link(href:="META-INF/resources/webjars/font-awesome/4.2.0/css/font-awesome.min.css", rel:="stylesheet"),
    link(href:=stylesName, rel:="stylesheet"),
    script(src:=scriptName),
    script("hljs.initHighlightingOnLoad();")
  )

  /**
   * The body of this site's HTML page
   */
  def bodyFrag(frag: Frag) = body(maxWidth:="768px", marginLeft:="auto", marginRight:="auto")(
    frag
  )

  /**
   * The contents of this page
   */
  def content: Map[String, Frag]

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
    for((path, frag) <- content){
      val txt = html(
        head(headFrags),
        bodyFrag(frag)
      ).render
      Files.write(
        Paths.get(outputRoot + path),
        txt.getBytes
      )
    }

  }
  def renderTo(outputRoot: String) = {
    new java.io.File(outputRoot).mkdirs()
    bundleResources(outputRoot)
    generateHtml(outputRoot)

  }
}

