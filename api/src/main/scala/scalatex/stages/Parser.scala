package scalatex
package stages
import acyclic.file
import scalaparse.Scala._
import scalaparse.syntax._
import fastparse._

/**
 * Parses the input text into a roughly-structured AST. This AST
 * is much simpler than the real Scala AST, but serves us well
 * enough until we stuff the code-strings into the real Scala
 * parser later
 */
object Parser extends ((String, Int) => Ast.Block){
  def apply(input: String, offset: Int = 0): Ast.Block = {
    new Parser(offset).Body0.parse(input) match {
      case s: fastparse.Result.Success[Ast.Block] => s.value
      case f: fastparse.Result.Failure => throw new Exception(f.trace)
    }
  }

}
class Parser(indent: Int = 0, offset: Int = 0) {
  implicit class FlatMapParser[T](p1: fastparse.Parser[T]){
    def flatMap[V](f: T => fastparse.Parser[V]): fastparse.Parser[V] = FlatMapped(p1, f)

  }

  /**
   * Wraps another parser, succeeding/failing identically
   * but consuming no input
   */
  case class LookaheadValue[T](p: fastparse.Parser[T]) extends fastparse.Parser[T]{
    def parseRec(cfg: ParseCtx, index: Int) = {
      p.parseRec(cfg, index) match{
        case s: fastparse.Result.Success.Mutable[T] => success(cfg.success, s.value, index, false)
        case f: fastparse.Result.Failure.Mutable => failMore(f, index, cfg.trace)
      }
    }
    override def toString = s"&($p)"
  }
  case class FlatMapped[T, V](p1: fastparse.Parser[T],
                              func: T => fastparse.Parser[V])
                              extends fastparse.Parser[V] {
    def parseRec(cfg: ParseCtx, index: Int): Result[V] = {
      p1.parseRec(cfg, index) match{
        case f: fastparse.Result.Failure.Mutable => failMore(f, index, cfg.trace, false)
        case s: fastparse.Result.Success.Mutable[T] => func(s.value).parseRec(cfg, s.index)
      }
    }
  }
  case object OffsetIndex extends fastparse.Parser[Int]{
    def parseRec(cfg: ParseCtx, index: Int) = {
      success(cfg.success, index + offset, index, false)
    }
  }

  def TextNot(chars: String): P[Ast.Block.Text] = P(
    OffsetIndex ~ (CharsWhile(!(chars + "\n").contains(_)) | "@@").rep1.!
  ).map{ case (i, x) => Ast.Block.Text(i, x.replace("@@", "@")) }
  val Text = TextNot("@")
  val Code = P( "@" ~ (scalaparse.syntax.Identifiers.Id | BlockExpr2 | ("(" ~ Exprs.? ~ ")")).! )
  val Header = P( "@" ~ (BlockDef | Import).! )

  val HeaderBlock: P[Ast.Header] = P(
    OffsetIndex ~ Header ~ (WL.! ~ Header map {case (a, b) => a + b}).rep ~
      IndentBlock
  ).map{
    case (i, start, heads, body) => Ast.Header(i, start + heads.mkString, body)
  }

  val BlankLine = P( "\n" ~ " ".rep ~ &("\n") )

  val IndentSpaces = P( fastparse.Parser.Repeat(" ", min = indent, delimiter = Pass) )
  val Indent = P( "\n" ~ IndentSpaces )
  val LoneScalaChain: P[(Ast.Block.Text, Ast.Chain)] = P(
    (OffsetIndex ~ (Indent | Start).!).map(Ast.Block.Text.tupled) ~
    (ScalaChain ~ IndentBlock).map{
      case (chain: Ast.Chain, body: Ast.Block) => chain.copy(parts = chain.parts :+ body)
    }
  )
  val IndentBlock: P[Ast.Block] = P(
    LookaheadValue("\n".rep ~ IndentSpaces.!) ~ OffsetIndex
  ).flatMap{ case (nextIndent, offsetIndex) => new Parser(nextIndent.length, offsetIndex).Body}

  val IfHead = P( "@" ~ (`if` ~ "(" ~ ExprCtx.Expr ~ ")").! )
  val IfElse1 = P(
    OffsetIndex ~ IfHead ~ BraceBlock ~ (`else` ~ (BraceBlock | IndentBlock)).?
  )
  val IfElse2 = P(
    (Indent| Start) ~ OffsetIndex ~ IfHead ~ IndentBlock ~ (Indent ~ "@else" ~ (BraceBlock | IndentBlock)).?
  )
  val IfElse: P[Ast.Block.IfElse] = P(
    (IfElse1 | IfElse2).map(Ast.Block.IfElse.tupled)
  )

  val ForHead = {
    val Body = P( "(" ~ ExprCtx.Enumerators ~ ")" | "{" ~ StatCtx.Enumerators ~ "}" )
    P( OffsetIndex ~ "@" ~ (`for` ~ Body).! )
  }
  val ForLoop: P[Ast.Block.For] = P(
    ForHead ~ BraceBlock
  ).map(Ast.Block.For.tupled)
  val LoneForLoop: P[(Ast.Block.Text, Ast.Block.For)] = P(
    (OffsetIndex ~ (Indent | Start).!).map(Ast.Block.Text.tupled) ~
    (ForHead ~ IndentBlock).map(Ast.Block.For.tupled)
  )

  val ScalaChain: P[Ast.Chain] = P(
    (OffsetIndex ~ Code ~ Extension.rep) map { case (x, c, ex) => Ast.Chain(x, c, ex) }
  )
  val Extension: P[Ast.Chain.Sub] = P(
    (OffsetIndex ~ "." ~ Identifiers.Id.! map Ast.Chain.Prop.tupled) |
    (OffsetIndex ~ TypeArgs2.! map Ast.Chain.TypeArgs.tupled) |
    (OffsetIndex ~ ArgumentExprs2.! map Ast.Chain.Args.tupled) |
    BraceBlock
  )
  val Ws = WL
  // clones of the version in ScalaSyntax, but without tailing whitespace or newlines
  val TypeArgs2 = P( "[" ~ Ws ~ Types ~ "]" )
  val ArgumentExprs2 = P(
    "(" ~ Ws ~
    ((Exprs ~ "," ~ Ws).? ~ ExprCtx.PostfixExpr ~ ":" ~ Ws ~ "_" ~ Ws ~ "*" ~ Ws | Exprs.? ) ~
    Ws ~ ")"
  )
  val BlockExpr2: P0 = P( "{" ~ Ws ~ (CaseClauses | Block) ~ Ws ~ "}" )
  val BraceBlock: P[Ast.Block] = P( "{" ~ BodyNoBrace ~ "}" )

  def BodyItem(exclusions: String) : P[Seq[Ast.Block.Sub]]  = P(
    ForLoop.map(Seq(_)) |
    LoneForLoop.map{ case (a, b) => Seq(a, b) } |
    IfElse.map(Seq(_)) |
    LoneScalaChain.map{ case (a, b) => Seq(a, b) } |
    HeaderBlock.map(Seq(_)) |
    TextNot("@" + exclusions).map(Seq(_)) |
    (OffsetIndex ~ Indent.!).map(Ast.Block.Text.tupled).map(Seq(_)) |
    (OffsetIndex ~ BlankLine.!).map(Ast.Block.Text.tupled).map(Seq(_)) |
    ScalaChain.map(Seq(_))
  )
  val Body = P( BodyEx() )
  val BodyNoBrace = P( BodyEx("}") )
  def BodyEx(exclusions: String = "") = P(
    OffsetIndex ~ BodyItem(exclusions).rep1 map {case (i, x) =>
      Ast.Block(i, flattenText(x.flatten))
    }
  )
  val Body0 = P(
    OffsetIndex ~ BodyItem("").rep map { case (i, x) =>
      Ast.Block(i, flattenText(x.flatten))
    }
  )
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
