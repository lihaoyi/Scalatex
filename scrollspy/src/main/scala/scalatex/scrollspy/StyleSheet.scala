package scalatex.scrollspy

import org.scalajs.dom
import org.scalajs.dom.css

import scalatags.JsDom.all._
import scalatags.JsDom.tags2

object ScrollSpyStyleSheet extends scalatags.stylesheet.StyleSheet{
  val menuItem = *(
    &hover(
      opacity := 0.9
    ),
    &active(
      opacity := 0.7
    ),
    display.block,
    textDecoration.none,
    paddingLeft := 15,
    height := 44,
    lineHeight := "44px",
    borderBottom := "1px solid #444"
  )
  val menuList = *(
    paddingLeft := "15px",
    margin := 0,
    overflow.hidden,
    position.relative,
    display.block,
    left := 0,
    top := 0,
    "transition".style := "maxHeight 0.2s ease-out"
  )

  val selected = *(
    backgroundColor := "#1f8dd6",
    color := "white"
  )

  val closed = *(color := "#999")

  val pathed = *(borderLeft := "2px solid white")

  lazy val styleTag = tags2.style.render
  dom.document.head.appendChild(styleTag)
  styleTag.textContent += this.styleSheetText
}
