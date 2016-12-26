package scalatex.site

import ammonite.ops._

import scalatags.Text.all._

object Sidebar {
  def snippet(tree: Seq[Tree[String]]) = script(raw(s"""
    scalatex.scrollspy.Controller().main(
      ${upickle.default.write(tree)}
  )"""))
  def autoResources = Seq(resource/'scalatex/'scrollspy/"scrollspy.js")
}