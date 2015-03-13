package scalatex.scrollspy

import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.html
import scalatags.JsDom.all._
object ScrollSpy{
  def set(el: dom.Element)(m: Modifier*): Unit ={
    m.foreach(_.applyTo(el))
  }
  val all = Seq(
    height := 44,
    lineHeight := "44px",
    borderBottom := "1px solid #444"
  )
  val selected = all ++ Seq(
    backgroundColor := "#1f8dd6",
    color := "white"
  )

  val fragStyle = all ++ Seq(
    backgroundColor := "",
    color := "#999"
  )
}



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
class ScrollSpy(structure: Tree[String]){
  import ScrollSpy._
  lazy val domTrees = {
    var i = -1
    def recurse(t: Tree[String], depth: Int): Tree[MenuNode] = {
      val link = a(
        t.value,
        display.block,
        textDecoration.none,
        paddingLeft := 15,
        href:="#"+Controller.munge(t.value),
        cls:="menu-item"
      ).render
      val originalI = i
      val children = t.children.map(recurse(_, depth + 1))

      val list = ul(
        paddingLeft := "15px",
        margin := 0,
        overflow.hidden,
        position.relative,
        display.block,
        left := 0,
        top := 0,
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

    val domTrees = recurse(structure, 0)
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
      rec(domTrees)(setFullHeight)
    }else{
      start(force = true)
    }
  }

  def setFullHeight(mn: MenuNode) = {
    mn.list.style.maxHeight = (mn.end - mn.start + 1) * 44 + "px"
  }

  def apply(): Unit = {
    start()
  }

  /**
   * Recurse over the navbar tree, opening and closing things as necessary
   */
  private[this] def start(force: Boolean = false) = {
    val scrollTop = dom.document.body.scrollTop

    def close(tree: Tree[MenuNode]): Unit = {
      tree.value.list.style.maxHeight = if (!open) "0px" else "none"
      tree.value.frag.style.borderLeft = ""
      tree.children.foreach(close)
      set(tree.value.link)(fragStyle)
    }
    def walk(tree: Tree[MenuNode]): Unit = {
      val epsilon = 10
      setFullHeight(tree.value)
      for((child, idx) <- tree.children.zipWithIndex) {
        if(offset(child.value.header) <= scrollTop + epsilon) {

          if (idx+1 >= tree.children.length || offset(tree.children(idx+1).value.header) > scrollTop + epsilon) {
            set(child.value.link)(selected)
            walk(child)
            child.value.frag.style.borderLeft = ""
          }else {
            close(child)
            child.value.frag.style.borderLeft = "2px solid white"
          }
        }else{
          child.value.frag.style.borderLeft = ""
          close(child)
        }
      }
    }
    set(domTrees.value.link)(selected)
    walk(domTrees)
  }
}