package scalatex.highlighter

object hl extends scalatex.site.Highlighter{
  def suffixMappings = Map(
    "scala" -> "scala",
    "sbt" -> "scala"
  )
}