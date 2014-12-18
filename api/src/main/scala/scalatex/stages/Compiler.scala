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

      trees.foreach(incPos(_, offset))
      trees
    }
    def incPos(tree: c.Tree, offset: Int): tree.type = {

      val current = if (tree.pos == NoPosition) 0 else tree.pos.point
      c.internal.setPos(tree,
        new OffsetPosition(
          fragPos.source,
          offset + current + fragPos.point
        ).asInstanceOf[c.universe.Position]
      )
      tree
    }

    def compileChain(code: String, parts: Seq[Ast.Chain.Sub], offset: Int): c.Tree = {

      val out = parts.foldLeft(incPosRec(c.parse(code), offset + 1)){
        case (curr, Ast.Chain.Prop(offset2, str)) =>
          incPos(q"$curr.${TermName(str)}", offset2 + 1)
        case (curr, Ast.Chain.Args(offset2, str)) =>
          val Apply(fun, args) = c.parse(s"omg$str")
          incPos(Apply(curr, args.map(incPosRec(_, offset2 - 2))), offset2)
        case (curr, Ast.Chain.TypeArgs(offset2, str)) =>
          val TypeApply(fun, args) = c.parse(s"omg$str")
          incPos(TypeApply(curr, args.map(incPosRec(_, offset2 - 2))), offset2)
        case (curr, Ast.Block(offset1, parts)) =>
          incPos(q"$curr(..${compileBlock(parts, offset1)})", offset1)
        case (curr, Ast.Header(offset1, header, block)) =>
          incPos(q"$curr(${compileHeader(header, block, offset1)})", offset1)
      }

      out
    }
    def compileBlock(parts: Seq[Ast.Block.Sub], offset: Int): Seq[c.Tree] = {
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

          val res = If(incPosRec(cond, offset1 + 2), compileBlockWrapped(parts2, offset2), elseCompiled)

          incPos(res, offset1)
          res
        case Ast.Block.For(offset1, generators, Ast.Block(offset2, parts2)) =>
          val fresh = c.fresh()

          val tree = incPosRec(c.parse(s"$generators yield $fresh"), offset1 + 2)

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
      val Block(stmts, expr) = c.parse(s"{$header\n ()}")
      Block(stmts, compileBlockWrapped(block.parts, block.offset))
    }

    val res = compileBlockWrapped(template.parts, template.offset)
    res
  }
}