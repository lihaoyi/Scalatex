package scalatex
package stages
import acyclic.file

import scalaparse.Scala
import scalaparse.Scala._
import scalaparse.syntax._
import fastparse._, NoWhitespace._

/**
 * Parses the input text into a roughly-structured AST. This AST
 * is much simpler than the real Scala AST, but serves us well
 * enough until we stuff the code-strings into the real Scala
 * parser later
 */
object Parser extends ((String, Int) => Parsed[Ast.Block]){
  def apply(input: String, offset: Int = 0): Parsed[Ast.Block] = {
    parse(input, new Parser(offset).File(_))
  }
}

class Parser(indent: Int = 0) {
  import scalaparse.syntax.{Key => K}

//  /**
//   * Wraps another parser, succeeding/failing identically
//   * but consuming no input
//   */
//  case class LookaheadValue[T](p: P[T]) extends P[T]{
//    def parseRec(cfg: fastparse.core.ParseCtx[Char, String], index: Int) = {
//      p.parseRec(cfg, index) match{
//        case s: Mutable.Success[T] => success(cfg.success, s.value, index, Set.empty, false)
//        case f: Mutable.Failure => failMore(f, index, cfg.logDepth)
//      }
//    }
//    override def toString = s"&($p)"
//  }

  /**
   * This only needs to parse the second `@`l the first one is
   * already parsed by [[BodyItem]]
   */
  def `@@`[_: P] = P( Index ~ "@" ).map(Ast.Block.Text(_, "@"))

  def TextNot[_: P](chars: String) = {
    def AllowedChars = CharsWhile(!(chars + "@\n").contains(_))
    P(Index ~ AllowedChars.!.rep(1)).map {
      case (i, x) => Ast.Block.Text(i, x.mkString)
    }
  }

  def Text[_: P] = TextNot("")
  def Code[_: P] = P( (scalaparse.syntax.Identifiers.Id | BlockExpr | ExprCtx.Parened ).! )
  def Header[_: P] = P( (BlockDef | Import).! )

  def HeaderBlock[_: P] = P( Index ~ Header ~ (WL.! ~ "@" ~ Header).rep ~ Body ).map{
    case (i, start, heads, body) => Ast.Header(i, start + heads.map{case (x, y) => x + y}.mkString, body)
  }

  def BlankLine[_: P] = P( "\n" ~ " ".rep ~ &("\n") )

  def IndentSpaces[_: P] = P( " ".rep(min = indent, sep = Pass) )
  def Indent[_: P] = P( "\n" ~ IndentSpaces )
  def IndentPrefix[_: P] = P( Index ~ (Indent | Start).! ).map(Ast.Block.Text.tupled)
  def IndentScalaChain[_: P] = P(ScalaChain ~ (IndentBlock | BraceBlock).?).map{
    case (chain, body) => chain.copy(parts = chain.parts ++ body)
  }

  def Deeper[_: P] = P( " ".rep(indent + 1) )
  def IndentBlock[_: P] = {
    ("\n" ~ Deeper.!).flatMapX { i =>
      val size = i.size
      val p = implicitly[P[_]]
      p.freshSuccessUnit(p.index - (size + 1))
      new Parser(indent = size).Body //actor.rep(1, sep = ("\n" + " " * i)./)
    }
  }

  //def block[_: P]: P[Int] = P( CharIn("+\\-*/").! ~/ blockBody).map(eval)

//  def IndentBlock[_: P] =
//    &("\n".rep(1) ~ IndentSpaces.!).flatMap { _ =>
//      P(("\n".rep(1) ~ IndentSpaces.!) ~ Index).flatMap {
//        case (nextIndent, offsetIndex) =>
//          if (nextIndent.length <= indent) Fail
//          else new Parser(nextIndent.length, offsetIndex).Body
//      }
//    }

  def IfHead[_: P] = P( (`if` ~/ "(" ~ ExprCtx.Expr ~ ")").! )
  def IfSuffix[_: P] = P( BraceBlock ~ (K.W("else") ~/ BraceBlock).?  )
  def IfElse[_: P] = P( Index ~ IfHead ~ IfSuffix).map { case (w, a, (b, c)) => Ast.Block.IfElse(w, a, b, c) }
  def IfBlockSuffix[_: P] = P( IndentBlock ~ (Indent ~ K.W("@else") ~ (BraceBlock | IndentBlock)).? )

  def IndentIfElse[_: P] = {
    P(Index ~ IfHead ~ (IfBlockSuffix | IfSuffix)).map {
      case (w, a, (b, c)) => Ast.Block.IfElse(w, a, b, c)
    }
  }

  def TTTT[_: P] = {
    P(Index ~ IfHead ~ IfBlockSuffix ).map {
      case (w, a, (b, c)) => Ast.Block.IfElse(w, a, b, c)
    }
  }


  def ForHead[_: P] = {
    def ForBody = P( "(" ~/ ExprCtx.Enumerators ~ ")" | "{" ~/ StatCtx.Enumerators ~ "}" )
    P( Index ~ (`for` ~/ ForBody).! )
  }
  def ForLoop[_: P] = P( ForHead ~ BraceBlock ).map(Ast.Block.For.tupled)
  def IndentForLoop[_: P] = P(
    (ForHead ~ (IndentBlock | BraceBlock)).map(Ast.Block.For.tupled)
  )

  def ScalaChain[_: P] = P( Index ~ Code ~ Extension.rep ).map {
    case (x, c, ex) => Ast.Chain(x, c, ex)
  }

  def Extension[_: P] = P(
    // Not cutting after the ".", because full-stops are very common
    // in english so this results in lots of spurious failures
    (Index ~ "." ~ Identifiers.Id.!).map(Ast.Chain.Prop.tupled) |
    (Index ~ TypeArgs.!).map(Ast.Chain.TypeArgs.tupled) |
    (Index ~ ParenArgList.!).map(Ast.Chain.Args.tupled) |
    BraceBlock
  )

  def BraceBlock[_: P] = P( "{" ~/ BodyNoBrace  ~ "}" )

  def CtrlFlow[_: P] = P( ForLoop | IfElse | ScalaChain | HeaderBlock | `@@` ).map(Seq(_))

  def CtrlFlowIndented[_: P] = P( IndentForLoop | IndentScalaChain | IndentIfElse | HeaderBlock | `@@` )

  def IndentedExpr[_: P] = P(
    (IndentPrefix ~ "@" ~/ CtrlFlowIndented).map{ case (a, b) => Seq(a, b) }
  )
  def BodyText[_: P](exclusions: String) = P(
    TextNot(exclusions).map(Seq(_)) |
    (Index ~ Indent.!).map(Ast.Block.Text.tupled).map(Seq(_)) |
    (Index ~ BlankLine.!).map(Ast.Block.Text.tupled).map(Seq(_))
  )
  def BodyItem[_: P](exclusions: String) : P[Seq[Ast.Block.Sub]]  = P(
    IndentedExpr |  "@" ~/ CtrlFlow | BodyText(exclusions)
  )
  def Body[_: P] = P( BodyEx("") )

  // Some day we'll turn this on, but for now it seems to be making things blow up
  def File[_: P] = P( Body/* ~ End */)
  def BodyNoBrace[_: P] = P( BodyEx("}") )
  def BodyEx[_: P](exclusions: String) =
    P( Index ~ BodyItem(exclusions).rep ).map {
      case (i, x) =>
        Ast.Block(i, flattenText(x.flatten))
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
