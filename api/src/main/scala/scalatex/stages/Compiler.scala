package scalatex
package stages

import acyclic.file

import scala.reflect.macros.whitebox.Context
import scala.reflect.internal.util.{Position, OffsetPosition}

/**
 * Walks the parsed AST, converting it into a structured Scala c.Tree
 */
object Compiler{

  def apply(c: Context)(fragPos: c.Position, template: Ast.Block): c.Tree = {

    import c.universe._
    def fragType = tq"scalatags.Text.all.Frag"

    def incPosRec(trees: c.Tree, offset: Int): trees.type = {

      trees.foreach { t => incPos(t, offset); () }
      trees
    }
    def incPos(tree: c.Tree, offset: Int): tree.type = {

      val current = if (tree.pos == NoPosition) 0 else tree.pos.point
      val finalPos =
        current + // start of of tree relative to start of fragment
        offset + // start of fragment relative to start of string-literal
        fragPos.point // start of string-literal relative to start of file

//      println(s"$finalPos = $current + $offset + ${fragPos.point}\t $tree")
      c.internal.setPos(tree,
        new OffsetPosition(
          fragPos.source,
          finalPos
        ).asInstanceOf[c.universe.Position]
      )
      tree
    }

    /**
     * We need something to prepend to our `.call` or `()`
     * or `[]` strings, so that they'll parse properly
     */
    val prefix = "omg"

    def compileChain(code: String, parts: Seq[Ast.Chain.Sub], offset: Int): c.Tree = {
//      println("compileChain " + parts + "\t" + offset)
      val out = parts.foldLeft(incPosRec(c.parse(code), offset)){
        case (curr, Ast.Chain.Prop(offset2, str)) =>
//          println(s"Prop $str $offset2")
          incPos(q"$curr.${TermName(str)}", offset2)

        case (curr, Ast.Chain.Args(offset2, str)) =>
          val t @ Apply(_, args) = c.parse(s"$prefix$str")
//          println(s"Args $str $offset2 ${t.pos.point}" )
          val offset3 = offset2 - prefix.length
          incPos(Apply(curr, args.map(incPosRec(_, offset3))), offset3 + t.pos.point)

        case (curr, Ast.Chain.TypeArgs(offset2, str)) =>
//          println(s"TypeArgs $str $offset2")
          val t @ TypeApply(_, args) = c.parse(s"$prefix$str")
          val offset3 = offset2 - prefix.length
          incPos(TypeApply(curr, args.map(incPosRec(_, offset3))), offset3 + t.pos.point)

        case (curr, Ast.Block(offset2, parts)) =>
//          println(s"Block $parts $offset2")
          // -1 because the offset of a block is currently defined as the
          // first character *after* the curly brace, where-as normal Scala
          // error messages place the error position for a block-apply on
          // the curly brace itself.
          incPos(q"$curr(..${compileBlock(parts, offset2-1)})", offset2 - 1)

        case (curr, Ast.Header(offset2, header, block)) =>
//          println(s"Header $header $offset2")
          incPos(q"$curr(${compileHeader(header, block, offset2)})", offset2)
      }

      out
    }
    def compileBlock(parts: Seq[Ast.Block.Sub], offset: Int): Seq[c.Tree] = {
//      println("compileBlock " + parts + "\t" + offset)
      val res = parts.map{
        case Ast.Block.Text(offset1, str) =>
          incPos(q"$str", offset1)

        case Ast.Chain(offset1, code, parts) =>
          compileChain(code, parts, offset1)

        case Ast.Header(offset1, header, block) =>
          compileHeader(header, block, offset1)

        case Ast.Block.IfElse(offset1, condString, Ast.Block(offset2, parts2), elseBlock) =>
          val If(cond, _, _) = c.parse(condString + "{}")
          val elseCompiled = elseBlock match{
            case Some(Ast.Block(offset3, parts3)) => compileBlockWrapped(parts3, offset3)
            case None => EmptyTree
          }

          val res = If(incPosRec(cond, offset1), compileBlockWrapped(parts2, offset2), elseCompiled)

          incPos(res, offset1)
          res

        case Ast.Block.For(offset1, generators, Ast.Block(offset2, parts2)) =>
          val fresh = c.fresh()

          val tree = incPosRec(c.parse(s"$generators yield $fresh"), offset1)

          def rec(t: Tree): Tree = t match {
            case a @ Apply(fun, List(f @ Function(vparams, body))) =>
              val f2 = Function(vparams, rec(body))
              val a2 = Apply(fun, List(f2))
              a2
            case Ident(x: TermName) if x.decoded == fresh =>
              compileBlockWrapped(parts2, offset2)
          }

          rec(tree)
      }
      res
    }
    def compileBlockWrapped(parts: Seq[Ast.Block.Sub], offset: Int): c.Tree = {
      incPos(q"Seq[$fragType](..${compileBlock(parts, offset)})", offset)
    }
    def compileHeader(header: String, block: Ast.Block, offset: Int): c.Tree = {
      val Block(stmts, _) = c.parse(s"{$header\n ()}")
      Block(stmts.map(incPosRec(_, offset)), compileBlockWrapped(block.parts, block.offset))
    }

    val res = compileBlockWrapped(template.parts, template.offset)
    res
  }
}
