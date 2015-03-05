import org.parboiled2.ParseError

import scala.reflect.internal.util.{BatchSourceFile, SourceFile, OffsetPosition}
import scala.reflect.io.{PlainFile, AbstractFile}
import scala.reflect.macros.{TypecheckException, Context}
import scalatags.Text.all._
import scalatex.stages.{Parser, Compiler}
import scala.language.experimental.macros
import acyclic.file

package object scalatex {
  /**
   * Converts the given string literal into a Scalatex fragment.
   */
  def tw(expr: String): Frag = macro Internals.applyMacro
  /**
   * Converts the given file into a Scalatex fragment.
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
      val txt = io.Source.fromFile(fileName).mkString
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
        expr.tree.pos.point + (if (stringStart == "\"\"") 1 else -1),
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
        val trimmed = stages.Trim(s)
        println("TRIMMED")
        println(trimmed)
        val parsed = try {
          Parser.tupled(trimmed)
        }catch{case e: ParseError =>
          println(e.traces(0))
          throw e
        }
        Compiler(c)(realPos, parsed)

      }
      println("SCALATEX SOURCE")
      println(scalatexSource)
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
