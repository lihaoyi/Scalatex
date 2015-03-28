package scalatex.site

import ammonite.ops._

import scalatags.Text.all._

object Sidebar {
  def snippet(tree: Seq[Tree[String]]) = script(raw(s"""
    scalatex.scrollspy.Controller().main(
      ${upickle.write(tree)}
  )"""))
  def autoResources = Seq(root/'scalatex/'scrollspy/"scrollspy.js")
}
