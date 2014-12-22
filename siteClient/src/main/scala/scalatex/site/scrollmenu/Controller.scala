package scalatex.site.scrollmenu

import org.scalajs.dom
import org.scalajs.dom.extensions._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._



@JSExport
object Controller{

  def munge(name: String) = {
    name.replace(" ", "")
  }

  @JSExport
  def main(data: scala.scalajs.js.Any) = {

    val structure = upickle.readJs[Tree[String]](upickle.json.readJs(data))(upickle.Reader.macroR)

    val Seq(main, menu, layout, menuLink) = Seq(
      "main", "menu", "layout", "menuLink"
    ).map(dom.document.getElementById)

    val snippets = dom.document.getElementsByClassName("highlight-me")

    snippets.foreach(js.Dynamic.global.hljs.highlightBlock(_))

    val scrollSpy = new ScrollSpy(structure, main)
    val list = ul(cls := "menu-item-list collapsed")(
      scrollSpy.domTrees.value.frag
    ).render

    def updateScroll() = scrollSpy()
    val expandIcon = i(cls := "fa fa-caret-down").render
    val expandLink =
      a(
        expandIcon,
        href := "javascript:",
        marginLeft := "0px",
        paddingLeft := "15px",
        paddingRight := "15px",
        position.absolute,
        top := "0px",
        right := "0px",
        cls := "pure-menu-selected",
        onclick := { (e: dom.Event) =>
          expandIcon.classList.toggle("fa-caret-down")
          expandIcon.classList.toggle("fa-caret-up")
          list.classList.toggle("collapsed")
          list.classList.toggle("expanded")
          scrollSpy.toggleOpen()
          //          updateScroll()
        }
      ).render


    menu.appendChild(
      div(cls := "pure-menu  pure-menu-open")(
        a(cls := "pure-menu-heading")(
          "Contents", expandLink
        ),
        list
      ).render
    )

    menuLink.onclick = (e: dom.MouseEvent) => {
      layout.classList.toggle("active")
      menu.classList.toggle("active")
      menuLink.classList.toggle("active")
    }

    main.onscroll = (e: dom.UIEvent) => updateScroll()
    updateScroll()

  }
}