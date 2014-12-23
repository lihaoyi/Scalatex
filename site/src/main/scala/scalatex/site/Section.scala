package scalatex.site

import scala.collection.mutable
import scalatags.Text.all
import scalatags.Text.all._


trait HeaderStrategy{
  def header(name: String, subname: String, anchor: Frag): ConcreteHtmlTag[String]
  def content(frag: Frag): Frag
}
object HeaderStrategy{
  def apply(h: (String, String, Frag) => ConcreteHtmlTag[String], c: Frag => Frag = f => f) = {
    new HeaderStrategy {
      def header(name: String, subname: String, anchor: Frag) = h(name, subname, anchor)
      def content(frag: all.Frag) = c(frag)
    }
  }
  implicit def TagToHeaderStrategy(t: ConcreteHtmlTag[String]): HeaderStrategy =
    HeaderStrategy((name, subname, frag) => t(name, frag))
}

case class Tree[T](value: T, children: mutable.Buffer[Tree[T]])

class Section{
  var structure = Tree[String]("root", mutable.Buffer.empty)
  var depth = 0
  val headers: Seq[HeaderStrategy] = Seq(h1, h2, h3, h4, h5, h6)
  val usedRefs = mutable.Set.empty[String]

  def ref(s: String, txt: String = "") = {
    usedRefs += s
    a(if (txt == "") s else txt, href:=s"#${munge(s)}")
  }
  def munge(name: String): String = name.replace(" ", "")
  def headingAnchor(name: String) = a(
    cls:="header-link",
    href:=s"#${munge(name)}",
    " ",
    i(cls:="fa fa-link")
  )
  def apply(header: String, subHeader: String = "") = {
    depth += 1
    val newNode = Tree[String](header, mutable.Buffer.empty)
    structure.children.append(newNode)
    val prev = structure
    structure = newNode
    new SectionProxy(body => {
      val hs = headers(depth - 1)
      val munged = munge(header)
      val res = Seq[Frag](
        hs.header(header, subHeader, headingAnchor(munged))(id:=munged),
        hs.content(body)
      )
      depth -= 1
      structure = prev
      res
    })
  }
}
class SectionProxy(func: Seq[Frag] => Frag){
  def apply(body: Frag*) = func(body)
}