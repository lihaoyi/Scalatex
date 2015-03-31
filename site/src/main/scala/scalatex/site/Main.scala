package scalatex.site

import ammonite.ops.{cwd, RelPath, Path}

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

  def main(args: Array[String]): Unit = renderTo(output)
  override def manualResources = super.manualResources ++ extraManualResources
  override def autoResources =
    super.autoResources ++
    hl.autoResources ++
    site.Sidebar.autoResources ++
    extraAutoResources


  val sect = new site.Section{}

  override def bodyFrag(frag: Frag) = {
    Seq(
      super.bodyFrag(frag),
      site.Sidebar.snippet(sect.structure.children),
      Highlighter.snippet
    )
  }
  def content = Map("index.html" -> frag)
}
