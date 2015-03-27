package scalatex

object Main extends site.Main(
  "https://github.com/lihaoyi/Scalatex/tree/master",
  ammonite.ops.cwd,
  ammonite.ops.cwd/'target/'site,
  Nil,
  scalatex.Readme()
)
