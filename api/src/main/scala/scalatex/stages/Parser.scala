package scalatex
package stages
import acyclic.file
import org.parboiled2._
import scalaParser.ScalaSyntax

/**
 * Parses the input text into a roughly-structured AST. This AST
 * is much simpler than the real Scala AST, but serves us well
 * enough until we stuff the code-strings into the real Scala
 * parser later
 */
object Parser extends ((String, Int) => Ast.Block){
  def apply(input: String, offset: Int = 0): Ast.Block = {
    new Parser(input, offset).Body0.run().get
  }
}
class Parser(input: ParserInput, indent: Int = 0, offset: Int = 0) extends scalaParser.ScalaSyntax(input) {
  def offsetCursor = offset + cursor
  val txt = input.sliceString(0, input.length)
  val indentTable = txt.split('\n').map{ s =>
    if (s.trim == "") -1
    else s.takeWhile(_ == ' ').length
  }
  val nextIndentTable = (0 until indentTable.length).map { i =>
    val index = indentTable.indexWhere(_ != -1, i + 1)
    if (index == -1) 100000
    else indentTable(index)
  }
  def cursorNextIndent() = {
    nextIndentTable(txt.take(cursor).count(_ == '\n'))
  }

  def TextNot(chars: String) = rule {
    push(offsetCursor) ~ capture(oneOrMore(noneOf(chars + "\n") | "@@")) ~> {
      (i, x) => Ast.Block.Text(i, x.replace("@@", "@"))
    }
  }
  def Text = TextNot("@")
  def Code = rule {
    "@" ~ capture(Identifiers.Id | BlockExpr2 | ('(' ~ optional(Exprs) ~ ')'))
  }
  def Header = rule {
    "@" ~ capture(Def | Import)
  }

  def HeaderBlock: Rule1[Ast.Header] = rule{
    push(offsetCursor) ~ Header ~ zeroOrMore(capture(WL) ~ Header ~> (_ + _)) ~ runSubParser{new Parser(_, indent, offsetCursor).Body0} ~> {
      (i: Int, start: String, heads: Seq[String], body: Ast.Block) => Ast.Header(i, start + heads.mkString, body)
    }
  }

  def BlankLine = rule{ '\n' ~ zeroOrMore(' ') ~ &('\n') }
  def IndentSpaces = rule{ indent.times(' ') ~ zeroOrMore(' ') }
  def Indent = rule{ '\n' ~ IndentSpaces }
  def LoneScalaChain: Rule2[Ast.Block.Text, Ast.Chain] = rule {
    (push(offsetCursor) ~ capture(Indent | test(cursor == 0)) ~> Ast.Block.Text) ~
    ScalaChain ~
    IndentBlock ~> {
      (chain: Ast.Chain, body: Ast.Block) => chain.copy(parts = chain.parts :+ body)
    }
  }
  def IndentBlock = rule{
    &("\n") ~
    test(cursorNextIndent() > indent) ~
    runSubParser(new Parser(_, cursorNextIndent(), offsetCursor).Body)
  }
  def IfHead = rule{ "@" ~ capture("if" ~ "(" ~ Expr ~ ")") }
  def IfElse1 = rule{
    push(offsetCursor) ~ IfHead ~ BraceBlock ~ optional("else" ~ (BraceBlock | IndentBlock))
  }
  def IfElse2 = rule{
    (Indent| test(cursor == 0)) ~ push(offsetCursor) ~ IfHead ~ IndentBlock ~ optional(Indent ~ "@else" ~ (BraceBlock | IndentBlock))
  }
  def IfElse = rule{
    (IfElse1 | IfElse2) ~> Ast.Block.IfElse
  }

  def ForHead = rule{
    push(offsetCursor) ~ "@" ~ capture("for" ~ '(' ~ Enumerators ~ ')')
  }
  def ForLoop = rule{
    ForHead ~
    BraceBlock ~> Ast.Block.For
  }
  def LoneForLoop = rule{
    (push(offsetCursor) ~ capture(Indent | test(cursor == 0)) ~> Ast.Block.Text) ~
    ForHead ~
    IndentBlock ~>
    Ast.Block.For
  }

  def ScalaChain = rule {
    push(offsetCursor) ~ Code ~ zeroOrMore(Extension) ~> ((x, c, ex) => Ast.Chain(x, c, ex))
  }
  def Extension: Rule1[Ast.Chain.Sub] = rule {
    (push(offsetCursor) ~ '.' ~ capture(Identifiers.Id) ~> Ast.Chain.Prop) |
    (push(offsetCursor) ~ capture(TypeArgs2) ~> Ast.Chain.TypeArgs) |
    (push(offsetCursor) ~ capture(ArgumentExprs2) ~> Ast.Chain.Args) |
    BraceBlock
  }
  def Ws = WL
  // clones of the version in ScalaSyntax, but without tailing whitespace or newlines
  def TypeArgs2 = rule { '[' ~ Ws ~ Types ~ ']' }
  def ArgumentExprs2 = rule {
    '(' ~ Ws ~
    (optional(Exprs ~ ',' ~ Ws) ~ PostfixExpr ~ ':' ~ Ws ~ '_' ~ Ws ~ '*' ~ Ws | optional(Exprs) ) ~
    Ws ~ ')'
  }
  def BlockExpr2: Rule0 = rule { '{' ~ Ws ~ (CaseClauses | Block) ~ Ws ~ '}' }
  def BraceBlock: Rule1[Ast.Block] = rule{ '{' ~ BodyNoBrace ~ '}' }

  def BodyItem(exclusions: String): Rule1[Seq[Ast.Block.Sub]] = rule{
    ForLoop ~> (Seq(_)) |
    LoneForLoop ~> (Seq(_, _)) |
    IfElse ~> (Seq(_)) |
    LoneScalaChain ~> (Seq(_, _)) |
    HeaderBlock ~> (Seq(_)) |
    TextNot("@" + exclusions) ~> (Seq(_)) |
    (push(offsetCursor) ~ capture(Indent) ~> Ast.Block.Text ~> (Seq(_))) |
    (push(offsetCursor) ~ capture(BlankLine) ~> Ast.Block.Text ~> (Seq(_))) |
    ScalaChain ~> (Seq(_: Ast.Block.Sub))
  }
  def Body = rule{ BodyEx() }
  def BodyNoBrace = rule{ BodyEx("}") }
  def BodyEx(exclusions: String = "") = rule{
    push(offsetCursor) ~ oneOrMore(BodyItem(exclusions)) ~> {(i, x) =>
      Ast.Block(i, flattenText(x.flatten))
    }
  }
  def Body0 = rule{
    push(offsetCursor) ~ zeroOrMore(BodyItem("")) ~> {(i, x) =>
      Ast.Block(i, flattenText(x.flatten))
    }
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
