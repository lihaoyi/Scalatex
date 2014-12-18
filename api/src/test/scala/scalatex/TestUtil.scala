package scalatex

import utest._


object TestUtil {
  implicit def stringify(f: scalatags.Text.all.Frag) = f.render
  def check(rendered: String*) = {
    val collapsed = rendered.map(collapse)
    val first = collapsed(0)
    assert(collapsed.forall(_ == first))
  }
  def collapse(s: String): String = {
    s.replaceAll("[ \n]", "")
  }
}
