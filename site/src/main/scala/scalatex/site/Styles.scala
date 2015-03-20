package scalatex.site
import scalatags.Text.all._
object Styles extends scalatags.stylesheet.CascadingStyleSheet {
  val headerLink = *(
    color := "#777",
    opacity := 0.05,
    textDecoration := "none"
  )
  val headerTag = *()

  val hoverContainer = *hover(
    headerLink(
      &hover(i(opacity := 1.0)),
      i(&active(opacity := 0.75)),
      i(opacity := 0.5)
    )

  )
  val content = *(
    position.relative,
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
  styleSheetText +=
    """
      |/*Workaround for bug in highlight.js IDEA theme*/
      |span.hljs-tag, span.hljs-symbol{
      |    background: none;
      |}
    """.stripMargin
}
