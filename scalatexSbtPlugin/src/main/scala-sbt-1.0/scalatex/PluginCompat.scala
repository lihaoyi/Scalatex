package scalatex

import sbt._
import sbt.internal.io.Source
object PluginCompat {
  implicit def fileToInternalSource(file: File): Source =
    new Source(file, AllPassFilter, NothingFilter)
}
