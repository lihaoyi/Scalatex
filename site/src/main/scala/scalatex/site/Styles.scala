package scalatex.site
import scalatags.Text.all._
import scalatags.stylesheet.{CascadingStyleSheet, Sheet}

object Styles{
  val css = Sheet[Styles]
}
trait Styles extends CascadingStyleSheet {

  def headerLink = cls(
    color := "#777",
    opacity := 0.05,
    textDecoration := "none"
  )

  def headerTag = cls()

  def hoverContainer = cls.hover(
    headerLink(
      headerLink.splice,
      &.hover(
        opacity := 1.0
      ),
      &.active(opacity := 0.75),
      opacity := 0.5
    )
  )

  def content = cls(
    *(
      position.relative
    ),
    maxWidth:="768px",
    marginLeft:="auto",
    marginRight:="auto",
    margin := "0 auto",
    padding := "0 1em",
    maxWidth := 800,
    paddingBottom := 50,
    lineHeight := "1.6em",
    color := "#777",
    p(
      textAlign.justify
    ),
    a(
      &link(
        color := "#37a",
        textDecoration := "none"
      ),
      &visited(
        color := "#949",
        textDecoration := "none"
      ),
      &hover(
        textDecoration := "underline"
      ),
      &active(
        color := "#000",
        textDecoration := "underline"
      )
    ),
    code(
      color := "#000"
    )

  )
  override def styleSheetText = super.styleSheetText +
    """
      |/*Workaround for bug in highlight.js IDEA theme*/
      |span.hljs-tag, span.hljs-symbol{
      |    background: none;
      |}
    """.stripMargin
}
