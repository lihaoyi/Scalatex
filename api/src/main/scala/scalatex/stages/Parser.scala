package scalatex
package stages
import acyclic.file

import scalaparse.Scala
import scalaparse.Scala._
import scalaparse.syntax._
import fastparse.all._

/**
 * Parses the input text into a roughly-structured AST. This AST
 * is much simpler than the real Scala AST, but serves us well
 * enough until we stuff the code-strings into the real Scala
 * parser later
 */
object Parser extends ((String, Int) => Parsed[Ast.Block]){
  def apply(input: String, offset: Int = 0): Parsed[Ast.Block] = {
    new Parser(offset).File.parse(input)
  }
}

class Parser(indent: Int = 0, offset: Int = 0) {
  import scalaparse.syntax.{Key => K}

  /**
   * Wraps another parser, succeeding/failing identically
   * but consuming no input
   */
  case class LookaheadValue[T](p: P[T]) extends P[T]{
    def parseRec(cfg: fastparse.core.ParseCtx[Char, String], index: Int) = {
      p.parseRec(cfg, index) match{
        case s: Mutable.Success[T] => success(cfg.success, s.value, index, Set.empty, false)
        case f: Mutable.Failure => failMore(f, index, cfg.logDepth)
      }
    }
    override def toString = s"&($p)"
  }

  /**
   * This only needs to parse the second `@`l the first one is
   * already parsed by [[BodyItem]]
   */
  def `@@` = P( Index ~ "@" ).map(Ast.Block.Text(_, "@"))
  def TextNot(chars: String) = {
    val AllowedChars = P( CharsWhile(!(chars + "@\n").contains(_)) )
    P( Index ~ AllowedChars.!.rep(1) ).map{
      case (i, x) => Ast.Block.Text(i, x.mkString)
    }
  }

  val Text = TextNot("")
  val Code = P( (scalaparse.syntax.Identifiers.Id | BlockExpr | ExprCtx.Parened ).! )
  val Header = P( (BlockDef | Import).! )

  val HeaderBlock = P( Index ~ Header ~ (WL.! ~ "@" ~ Header).rep ~ Body ).map{
    case (i, start, heads, body) => Ast.Header(i, start + heads.map{case (x, y) => x + y}.mkString, body)
  }

  val BlankLine = P( "\n" ~ " ".rep ~ &("\n") )

  val IndentSpaces = P( " ".rep(min = indent, sep = Pass) )
  val Indent = P( "\n" ~ IndentSpaces )
  val IndentPrefix = P( Index ~ (Indent | Start).! ).map(Ast.Block.Text.tupled)
  val IndentScalaChain = P(ScalaChain ~ (IndentBlock | BraceBlock).?).map{
    case (chain, body) => chain.copy(parts = chain.parts ++ body)
  }

  val IndentBlock = P( LookaheadValue("\n".rep(1) ~ IndentSpaces.!) ~ Index ).flatMap{
    case (nextIndent, offsetIndex) =>
      if (nextIndent.length <= indent) Fail
      else new Parser(nextIndent.length, offsetIndex).Body
  }

  val IfHead = P( (`if` ~/ "(" ~ ExprCtx.Expr ~ ")").! )
  val IfSuffix = P( BraceBlock ~ (K.W("else") ~/ BraceBlock).?  )
  val IfElse = P( Index ~ IfHead ~ IfSuffix).map{ case (w, a, (b, c)) => Ast.Block.IfElse(w, a, b, c) }

  val IndentIfElse = {
    val IfBlockSuffix = P( IndentBlock ~ (Indent ~ K.W("@else") ~ (BraceBlock | IndentBlock)).? )
    P(Index ~ IfHead ~ (IfBlockSuffix | IfSuffix)).map{
      case (w, a, (b, c)) => Ast.Block.IfElse(w, a, b, c)
    }
  }


  val ForHead = {
    val ForBody = P( "(" ~/ ExprCtx.Enumerators ~ ")" | "{" ~/ StatCtx.Enumerators ~ "}" )
    P( Index ~ (`for` ~/ ForBody).! )
  }
  val ForLoop = P( ForHead ~ BraceBlock ).map(Ast.Block.For.tupled)
  val IndentForLoop = P(
    (ForHead ~ (IndentBlock | BraceBlock)).map(Ast.Block.For.tupled)
  )

  val ScalaChain = P( Index ~ Code ~ Extension.rep ).map {
    case (x, c, ex) => Ast.Chain(x, c, ex)
  }

  val Extension = P(
    // Not cutting after the ".", because full-stops are very common
    // in english so this results in lots of spurious failures
    (Index ~ "." ~ Identifiers.Id.!).map(Ast.Chain.Prop.tupled) |
    (Index ~ TypeArgs.!).map(Ast.Chain.TypeArgs.tupled) |
    (Index ~ ParenArgList.!).map(Ast.Chain.Args.tupled) |
    BraceBlock
  )

  val BraceBlock = P( "{" ~/ BodyNoBrace  ~ "}" )

  val CtrlFlow = P( ForLoop | IfElse | ScalaChain | HeaderBlock | `@@` ).map(Seq(_))

  val CtrlFlowIndented = P( IndentForLoop | IndentScalaChain | IndentIfElse | HeaderBlock | `@@` )

  val IndentedExpr = P(
    (IndentPrefix ~ "@" ~/ CtrlFlowIndented).map{ case (a, b) => Seq(a, b) }
  )
  def BodyText(exclusions: String) = P(
    TextNot(exclusions).map(Seq(_)) |
    (Index ~ Indent.!).map(Ast.Block.Text.tupled).map(Seq(_)) |
    (Index ~ BlankLine.!).map(Ast.Block.Text.tupled).map(Seq(_))
  )
  def BodyItem(exclusions: String) : P[Seq[Ast.Block.Sub]]  = P(
    IndentedExpr |
    "@" ~/ CtrlFlow |
    BodyText(exclusions)
  )
  val Body = P( BodyEx("") )

  // Some day we'll turn this on, but for now it seems to be making things blow up
  val File = P( Body/* ~ End */)
  val BodyNoBrace = P( BodyEx("}") )
  def BodyEx(exclusions: String) = P( Index ~ BodyItem(exclusions).rep ).map{
    case (i, x) => Ast.Block(i, flattenText(x.flatten))
  }

  def flattenText(seq: Seq[Ast.Block.Sub]) = {
    seq.foldLeft(Seq.empty[Ast.Block.Sub]){
      case (prev :+ Ast.Block.Text(offset, txt1), Ast.Block.Text(_, txt2)) =>
        prev :+ Ast.Block.Text(offset, txt1 + txt2)
      case (prev, next) => prev :+ next
    }
  }
}

trait Ast{
  def offset: Int
}
object Ast{

  /**
   * @param parts The various bits of text and other things which make up this block
   * @param offset
   */
  case class Block(offset: Int, parts: Seq[Block.Sub]) extends Chain.Sub with Block.Sub
  object Block{
    trait Sub extends Ast
    case class Text(offset: Int, txt: String) extends Block.Sub
    case class For(offset: Int, generators: String, block: Block) extends Block.Sub
    case class IfElse(offset: Int, condition: String, block: Block, elseBlock: Option[Block]) extends Block.Sub
  }
  case class Header(offset: Int, front: String, block: Block) extends Block.Sub with Chain.Sub

  /**
   * @param lhs The first expression in this method-chain
   * @param parts A list of follow-on items chained to the first
   * @param offset
   */
  case class Chain(offset: Int, lhs: String, parts: Seq[Chain.Sub]) extends Block.Sub
  object Chain{
    trait Sub extends Ast
    case class Prop(offset: Int, str: String) extends Sub
    case class TypeArgs(offset: Int, str: String) extends Sub
    case class Args(offset: Int, str: String) extends Sub
  }


}
