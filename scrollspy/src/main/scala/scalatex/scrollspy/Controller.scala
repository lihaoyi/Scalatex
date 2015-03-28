package scalatex.scrollspy

import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.html

import scalajs.js
import scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import scalatags.JsDom.tags2
import Styles.css

class Toggler(var open: Boolean,
              menu: => html.Element,
              body: => html.Element,
              all: => html.Element){

  def toggle() = open = !open

  def apply() = {
    body.style.transition = "margin-left 0.2s ease-out"
    val width = if (open) "250px" else s"${Styles.itemHeight}px"
    all.style.opacity = if (open) "1.0" else "0.0"
    menu.style.width = width
    body.style.marginLeft = width
  }
}

@JSExport
object Controller{
  lazy val styleTag = tags2.style.render
  dom.document.head.appendChild(styleTag)
  styleTag.textContent += css.styleSheetText

  def munge(name: String) = {
    name.replace(" ", "")
  }

  @JSExport
  def main(data: js.Any) = {
    (new Controller(data)).init()
  }
}

class Controller(data: js.Any){


  val scrollSpy = new ScrollSpy(
    upickle.readJs[Tree[String]](upickle.json.readJs(data))
  )

  val list = ul(
    cls := "menu-item-list",
    margin := 0,
    padding := 0,
    "flex".style := 10000,
    scrollSpy.domTrees.value.frag
  ).render

  def updateScroll() = scrollSpy()

  def toggleButton(clsA: String, clsB: String, action: () => Unit, mods: Modifier*) = {
    val icon = i(
      cls := "fa " + clsA,
      color := "white"
    ).render
    val link = a(
      icon,
      href := "javascript:",
      Styles.css.menuLink,
      onclick := { (e: dom.Event) =>
        icon.classList.toggle(clsA)
        icon.classList.toggle(clsB)
        action()
      },
      mods
    )
    link
  }

  val expandLink = toggleButton(
    "fa-caret-down", "fa-caret-up",
    () => scrollSpy.toggleOpen(),
    right := Styles.itemHeight
  ).render

  val initiallyOpen = dom.innerWidth > 800
  val toggler = new Toggler(initiallyOpen, menu, dom.document.body, all)

  val openLink = {
    val (startCls, altCls) =
      if (initiallyOpen) ("fa-caret-left", "fa-caret-right")
      else ("fa-caret-right", "fa-caret-left")

    toggleButton(
      startCls, altCls,
      () => {toggler.toggle(); toggler.apply()}
    )(right := "0px").render
  }

  def init() = {
    dom.document.getElementsByClassName("highlight-me").foreach(
      js.Dynamic.global.hljs.highlightBlock(_)
    )
    updateScroll()

    toggler.apply()
    dom.document.body.appendChild(menu)

    dom.addEventListener("scroll", (e: dom.UIEvent) => updateScroll())
  }

  val footer = div(
    Styles.css.noteBox,
    a(
      "Published using Scalatex",
      href:="https://lihaoyi.github.io/Scalatex",
      Styles.css.note
    )
  )
  val all = div(
    display := "flex",
    "flex-direction".style := "column",
    minHeight := "100%",
    transition := "opacity 0.2s ease-out",
    list,
    expandLink,
    footer
  ).render

  val menu: html.Element = div(
    Styles.css.menu,
    all,
    openLink
  ).render
}