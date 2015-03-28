package scalatex.scrollspy

import scalatags.JsDom.all._
import scalatags.stylesheet.{Sheet, StyleSheet}

object Styles{
  val itemHeight = 44
  val selectedColor = "#1f8dd6"
  val css = Sheet[Styles]
  val menuBackground = "#191818"
}
trait Styles extends StyleSheet{

  def noteBox = cls(
    height := "14px",
    textAlign.right,
    bottom := 0,
    paddingTop := 5,
    paddingRight := 5,
    paddingBottom := 2,
    backgroundColor := Styles.menuBackground
  )

  def note = cls(
    fontSize := "12px",
    color := "#555",
    textDecoration.none,
    fontStyle.italic,
    &hover(
      color := "#777"
    ),
    &active(
      color := "#999"
    )
  )
  def menu = cls(
    position.fixed,
    overflow.scroll,
    whiteSpace.nowrap,
    backgroundColor := Styles.menuBackground,
    transition := "width 0.2s ease-out",
    height := "100%",
    left := 0,
    top := 0
  )
  def menuLink = cls(
    position.absolute,
    top := "0px",
    height := Styles.itemHeight,
    width := Styles.itemHeight,
    display := "flex",
    "align-items".style := "center",
    "justify-content".style := "center",
    textDecoration.none,
    selected.splice
  )
  def menuItem = cls(
    &hover(
      color := "#bbb",
      backgroundColor := "#292828"
    ),
    &active(
      color := "#ddd",
      backgroundColor := "#393838"
    ),
    display.block,
    textDecoration.none,
    paddingLeft := 15,
    height := Styles.itemHeight,
    lineHeight := "44px",
    borderBottom := "1px solid #444"
  )
  def menuList = cls(
    paddingLeft := "15px",
    margin := 0,
    overflow.hidden,
    position.relative,
    display.block,
    left := 0,
    top := 0,
    transition := "max-height 0.2s ease-out"
  )

  def selected = cls(
    backgroundColor := Styles.selectedColor,
    &hover(
      backgroundColor := "#369FE2",
      color := "white"
    ),
    &active(
      backgroundColor := "#62b4e8",
      color := "white"
    ),
    color := "white"
  )

  def closed = cls(color := "#999")

  def pathed = cls(borderLeft := "2px solid white")

  def exact = cls(
    fontStyle.italic
  )
}
