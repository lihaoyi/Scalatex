package scalatex.scrollspy

import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.html
import scalatags.JsDom.all._

case class Tree[T](value: T, children: Vector[Tree[T]])

case class MenuNode(frag: html.Element,
                    link: html.Element,
                    list: html.Element,
                    header: html.Element,
                    id: String,
                    start: Int,
                    end: Int)

/**
 * High performance scalatex.scrollspy to work keep the left menu bar in sync.
 * Lots of sketchy imperative code in order to maximize performance.
 */
class ScrollSpy(structure: Seq[Tree[String]]){

  lazy val domTrees = {
    var i = -1
    def recurse(t: Tree[String], depth: Int): Tree[MenuNode] = {
      val link = a(
        t.value,
        href:="#"+Controller.munge(t.value),
        cls:="menu-item",
        Styles.menuItem
      ).render
      val originalI = i
      val children = t.children.map(recurse(_, depth + 1))

      val list = ul(
        Styles.menuList,
        children.map(_.value.frag)
      ).render

      val curr = li(
        display.block,
        link,
        list
      ).render

      i += 1

      Tree(
        MenuNode(
          curr,
          link,
          list,
          dom.document.getElementById(Controller.munge(t.value)).asInstanceOf[html.Element],
          Controller.munge(t.value),
          originalI,
          if (children.length > 0) children.map(_.value.end).max else originalI + 1
        ),
        children
      )
    }

    val domTrees = structure.map(recurse(_, 0))
    domTrees
  }
  def offset(el: html.Element): Double = {
    val parent = dom.document.body
    if (el == parent) 0
    else el.offsetTop + offset(el.offsetParent.asInstanceOf[html.Element])
  }

  var open = false
  def toggleOpen() = {
    open = !open
    if (open){
      def rec(tree: Tree[MenuNode])(f: MenuNode => Unit): Unit = {
        f(tree.value)
        tree.children.foreach(rec(_)(f))
      }
      domTrees.map(rec(_)(setFullHeight))
    }else{
      start(force = true)
    }
  }

  def setFullHeight(mn: MenuNode) = {
    // height + 1 to account for border
    mn.list.style.maxHeight = (mn.end - mn.start + 1) * (Styles.itemHeight + 1) + "px"
  }

  def apply(): Unit = {
    start()
  }

  /**
   * Recurse over the navbar tree, opening and closing things as necessary
   */
  private[this] def start(force: Boolean = false) = {
    val scrollTop = {
      // Fix #21:
      // document.documentElement.scrollTop is the correct cross-browser way to get main window scroll offset
      // see discussion: https://groups.google.com/a/chromium.org/forum/#!msg/blink-dev/75hVThfrJ08/YD9kSgQljFEJ
      // however it is broken in current Chrome: https://code.google.com/p/chromium/issues/detail?id=157855
      // so the document.body workaround must be used for now
      val docScrollTop = dom.document.documentElement.scrollTop
      if (docScrollTop > 0) docScrollTop else dom.document.body.scrollTop
    }

    def close(tree: Tree[MenuNode]): Unit = {
      if (!open) tree.value.list.style.maxHeight = "0px"
      else setFullHeight(tree.value)
      tree.value.frag.classList.remove(Styles.pathed.name)

      tree.children.foreach(close)
      tree.value.link.classList.add(Styles.closed.name)
      tree.value.link.classList.remove(Styles.selected.name)
    }
    def walk(tree: Tree[MenuNode]): Unit = {
      setFullHeight(tree.value)
      recChildren(tree.children)
    }
    def recChildren(children: Seq[Tree[MenuNode]]) = {
      val epsilon = 10

      for((child, idx) <- children.zipWithIndex) {
        if(offset(child.value.header) <= scrollTop + epsilon) {
          if (idx+1 >= children.length || offset(children(idx+1).value.header) > scrollTop + epsilon) {
            child.value.link.classList.remove(Styles.closed.name)
            child.value.link.classList.add(Styles.selected.name)
            walk(child)
            child.value.frag.classList.remove(Styles.pathed.name)
          }else {

            close(child)
            child.value.frag.classList.add(Styles.pathed.name)
          }
        }else{
          child.value.frag.classList.remove(Styles.pathed.name)
          close(child)
        }
      }
    }

    recChildren(domTrees)
    for(t <- domTrees){
      val cl = t.value.link.classList
      setFullHeight(t.value)
      cl.remove(Styles.closed.name)
      t.value.frag.classList.remove(Styles.pathed.name)
      cl.add(Styles.selected.name)
    }
  }
}