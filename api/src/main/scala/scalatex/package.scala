

import scala.reflect.internal.util.{BatchSourceFile, SourceFile, OffsetPosition}
import scala.reflect.io.{PlainFile, AbstractFile}
import scala.reflect.macros.{TypecheckException, Context}
import scalatags.Text.all._
import scalatex.stages.{Ast, Parser, Compiler}
import scala.language.experimental.macros
import acyclic.file
import fastparse.all._

package object scalatex {
  /**
   * Converts the given string literal into a Scalatex fragment.
   */
  def tw(expr: String): Frag = macro Internals.applyMacro
  /**
   * Converts the given file into Pa Scalatex fragment.
   */
  def twf(expr: String): Frag = macro Internals.applyMacroFile
  object Internals {

    def twRuntimeErrors(expr: String): Frag = macro applyMacroRuntimeErrors
    def twfRuntimeErrors(expr: String): Frag = macro applyMacroFileRuntimeErrors

    def applyMacro(c: Context)(expr: c.Expr[String]): c.Expr[Frag] = applyMacroFull(c)(expr, false, false)
    def applyMacroRuntimeErrors(c: Context)(expr: c.Expr[String]): c.Expr[Frag] = applyMacroFull(c)(expr, true, false)
    def applyMacroFile(c: Context)(expr: c.Expr[String]): c.Expr[Frag] = applyMacroFileBase(c)(expr, false)
    def applyMacroFileRuntimeErrors(c: Context)(expr: c.Expr[String]): c.Expr[Frag] = applyMacroFileBase(c)(expr, true)


    def applyMacroFileBase(c: Context)(filename: c.Expr[String], runtimeErrors: Boolean): c.Expr[Frag] = {
      import c.universe._
      val fileName = filename.tree
        .asInstanceOf[Literal]
        .value
        .value
        .asInstanceOf[String]
      val txt = io.Source.fromFile(fileName)(scala.io.Codec.UTF8).mkString
      val sourceFile = new BatchSourceFile(
        new PlainFile(fileName),
        txt.toCharArray
      )
      compileThing(c)(txt, sourceFile, 0, runtimeErrors, false)

    }
    case class DebugFailure(msg: String, pos: String) extends Exception(msg)

    private[this] def applyMacroFull(c: Context)
                      (expr: c.Expr[String],
                       runtimeErrors: Boolean,
                       debug: Boolean)
                      : c.Expr[Frag] = {
      import c.universe._
      val scalatexFragment = expr.tree
                  .asInstanceOf[Literal]
                  .value
                  .value
                  .asInstanceOf[String]
      val stringStart =
        expr.tree
          .pos
          .lineContent
          .drop(expr.tree.pos.column)
          .take(2)
      compileThing(c)(
        scalatexFragment,
        expr.tree.pos.source,
        expr.tree.pos.point
        + (if (stringStart == "\"\"") 3 else 1) // Offset from start of string literal
        - 1, // WTF I don't know why we need this
        runtimeErrors,
        debug
      )
    }
    def compileThing(c: Context)
                    (scalatexSource: String,
                     source: SourceFile,
                     point: Int,
                     runtimeErrors: Boolean,
                     debug: Boolean) = {
      import c.universe._
      def compile(s: String): c.Tree = {
        val realPos = new OffsetPosition(source, point).asInstanceOf[c.universe.Position]
        val input = stages.Trim(s)
        Parser.tupled(input) match {
          case s: Parsed.Success[Ast.Block] => Compiler(c)(realPos, s.value)
          case f: Parsed.Failure =>
            val lines = input._1.take(f.index).lines.toVector
            throw new TypecheckException(
              new OffsetPosition(source, point + f.index).asInstanceOf[c.universe.Position],
              "Syntax error, expected (" + f.extra.traced.traceParsers.mkString(" | ") + ")"+
              "\n at line " + lines.length +
              " column " + lines.last.length +
              " index " + f.index
            )
        }

      }

      import c.Position
      try {
        val compiled = compile(scalatexSource)
        if (debug) println(compiled)
        c.Expr[Frag](c.typeCheck(compiled))
      } catch {
        case e@TypecheckException(pos: Position, msg) =>
          if (!runtimeErrors) c.abort(pos, msg)
          else {
            val posMsg = pos.lineContent + "\n" + (" " * pos.column) + "^"
            c.Expr( q"""throw scalatex.Internals.DebugFailure($msg, $posMsg)""")
          }
      }
    }
  }
}
