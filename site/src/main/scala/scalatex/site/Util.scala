package scalatex.site

import scala.collection.mutable

case class Tree[T](value: T, children: mutable.Buffer[Tree[T]])