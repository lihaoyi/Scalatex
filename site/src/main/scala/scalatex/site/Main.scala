package scalatex.site

import java.util.concurrent.Executors

import ammonite.ops.{cwd, RelPath, Path}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.util.{Success, Failure, Try}
import scalaj.http._
import scalatags.Text.all._
import scalatex.site


/**
 * A default `Main` implementation you can subclass if you do not need
 * the flexibility given by constructing your [[Site]] manually.
 *
 * Hooks up all the common components (Highlighter, Section, etc) in a
 * common configuration.
 */
class Main(url: String,
           val wd: Path,
           output: Path,
           extraAutoResources: Seq[Path],
           extraManualResources: Seq[Path],
           frag: => Frag) extends scalatex.site.Site{

  lazy val hl = new Highlighter {
    override def pathMappings = Seq(
      wd -> url
    )
  }

  def main(args: Array[String]): Unit = {
    renderTo(output)
    val unknownRefs = sect.usedRefs.filterNot(sect.headerSeq.contains)
    assert(
      unknownRefs.isEmpty,
      s"Unknown sections referred to by your `sect.ref` calls: $unknownRefs"
    )
    if (args.contains("--validate")){
      val tp = Executors.newFixedThreadPool(100)
      try{
        implicit val ec = ExecutionContext.fromExecutorService(
          tp
        )
        import concurrent.duration._
        println("Validating links")

        val codes = for(link <- usedLinks) yield (
          link,
          Future{
            Http(link).timeout(connTimeoutMs = 5000, readTimeoutMs = 5000).asBytes.code
          }
        )

        val results = codes.map{ case (link, f) => (link, Try(Await.result(f, 10.seconds)))}
        val failures = results.collect{
          case (link, Failure(exc)) => (link, exc)
          case (link, Success(x)) if x >= 400 => (link, new Exception("Return code " + x))
        }
        if (failures.length > 0){
          val failureText =
            failures.map{case (link, exc) => link + "\n\t" + exc}.mkString("\n")
          throw new Exception("Invalid links found in site\n" + failureText)
        }
        println("Links OK")
      }finally{
        tp.shutdown()
      }
    }
  }

  override def manualResources = super.manualResources ++ extraManualResources
  override def autoResources =
    super.autoResources ++
    hl.autoResources ++
    site.Sidebar.autoResources ++
    extraAutoResources


  val sect = new site.Section{}

  /**
    * Default
    */
  override def pageTitle = {
    println(sect.headerSeq.lift(1))
    sect.headerSeq.lift(1)
  }
  override def bodyFrag(frag: Frag) = {
    Seq(
      super.bodyFrag(frag),
      site.Sidebar.snippet(sect.structure.children),
      Highlighter.snippet
    )
  }
  def content = {
    /**
      * Precompute so we have the set of headers ready, since we use the first
      * header to use as the title of the page
      */
    val precalcFrag = frag
    Map("index.html" -> (defaultHeader, precalcFrag))
  }

  val usedLinks = collection.mutable.Buffer.empty[String]
  def lnk(name: String, customUrl: String = "") = {
    val usedUrl = if (customUrl == "") name else customUrl
    usedLinks.append(usedUrl)
    a(name, href := usedUrl)
  }
}
