package scalaParser
import acyclic.file
import language.implicitConversions
import syntax._
import org.parboiled2._

/**
 * Parser for Scala syntax.
 *
 * The `G` parameter that gets passed in to each rule stands for
 * "Greedy", and determines whether or not that rule is to consume
 * newlines after the last terminal in that rule. We need to pass it
 * everywhere so it can go all the way to the last terminal deep
 * inside the parse tree, which can then decide whether or not to
 * consume whitespace.
 *
 * The vast majority of terminals will consume newlines; only rules
 * which occur in {} blocks won't have their terminals consume newlines,
 * and only the *last* terminal in the rule will be affected.
 * That's why the parser does terminals-consume-newlines-by-default,
 * and leaves it up to the dev to thread the `G` variable where-ever
 * we want the opposite behavior.
 */
class ScalaSyntax(val input: ParserInput) extends Parser with Basic with Identifiers with Literals {
  // Aliases for common things. These things are used in almost every parser
  // in the file, so it makes sense to keep them short.
  type B = Boolean
  val t = true
  type R0 = Rule0
  /**
   * Parses all whitespace, excluding newlines. This is only
   * really useful in e.g. {} blocks, where we want to avoid
   * capturing newlines so semicolon-inference would work
   */
  def WS = rule { zeroOrMore(Basic.WhitespaceChar | Literals.Comment) }

  /**
   * Parses whitespace, including newlines.
   * This is the default for most things
   */
  def WL = rule{ zeroOrMore(Basic.WhitespaceChar | Literals.Comment | Basic.Newline) }



  /**
   * By default, all strings and characters greedily
   * capture all whitespace immediately after the token.
   */
  implicit private[this] def wspStr(s: String): R0 = rule { WL ~ str(s)  }
  implicit private[this] def wspChar(s: Char): R0 = rule { WL ~ ch(s) }

  /**
   * Most keywords don't just require the correct characters to match,
   * they have to ensure that subsequent characters *don't* match in
   * order for it to be a keyword. This enforces that rule for key-words
   * (W) and key-operators (O) which have different non-match criteria.
   */
  object K {
    def W(s: String) = rule {
      WL ~ Key.W(s)
    }

    def O(s: String) = rule {
      WL ~ Key.O(s)
    }
  }


  def pos = cursor -> cursorChar

  /**
   * helper printing function
   */
  def pr(s: String) = rule { run(println(s"LOGGING $cursor: $s")) }

  def Id = rule { WL ~ Identifiers.Id }
  def VarId = rule { WL ~ Identifiers.VarId }
  def Literal = rule { WL ~ Literals.Literal }
  def Semi = rule { WS ~ Basic.Semi }
  def Semis = rule { oneOrMore(Semi) }
  def Newline = rule { WL ~ Basic.Newline }

  def QualId = rule { WL ~ oneOrMore(Id).separatedBy('.') }
  def Ids = rule { oneOrMore(Id) separatedBy ',' }

  def Path: R0 = rule {
    zeroOrMore(Id ~ '.') ~ K.W("this") ~ zeroOrMore(Id).separatedBy('.') |
      StableId
  }
  def StableId: R0 = rule {
    zeroOrMore(Id ~ '.') ~ (K.W("this") | K.W("super") ~ optional(ClassQualifier)) ~ '.' ~ oneOrMore(Id).separatedBy('.') |
      Id ~ zeroOrMore(WL ~ '.' ~ WL ~ Id)
  }

  def ClassQualifier = rule { '[' ~ Id ~ ']' }

  def Type: R0 = rule {
    FunctionArgTypes ~ K.O("=>") ~ Type | InfixType ~ optional(WL ~ ExistentialClause)
  }
  def FunctionArgTypes = rule {
    InfixType | '(' ~ optional(oneOrMore(ParamType) separatedBy ',') ~ ')'
  }

  def ExistentialClause = rule { "forSome" ~ '{' ~ oneOrMore(ExistentialDcl).separatedBy(Semi) }
  def ExistentialDcl = rule { K.W("type") ~ TypeDcl | K.W("val") ~ ValDcl }

  def InfixType = rule {
    CompoundType ~ zeroOrMore(WL ~ Id ~ optional(Newline) ~ CompoundType)
  }
  def CompoundType = rule {
    oneOrMore(AnnotType).separatedBy(WL ~ K.W("with")) ~ optional(Refinement)
  }
  def AnnotType = rule {
    SimpleType ~ zeroOrMore(WL ~ Annotation)
  }
  def SimpleType: R0 = rule {
    BasicType ~
      optional(WL ~ '#' ~ Id) ~
      optional(WL ~ TypeArgs)
  }
  def BasicType: R0 = rule {
    '(' ~ Types ~ ')' |
      Path ~ '.' ~ K.W("type") |
      StableId
  }
  def TypeArgs = rule { '[' ~ Types ~ "]" }
  def Types = rule { oneOrMore(Type).separatedBy(',') }
  def Refinement = rule {
    optional(Newline) ~ '{' ~ oneOrMore(RefineStat).separatedBy(Semi) ~ "}"
  }
  def RefineStat = rule { "type" ~ TypeDef | Dcl | MATCH }
  def TypePat = rule { CompoundType }
  def Ascription = rule {
    ":" ~ ("_" ~ "*" | InfixType | oneOrMore(Annotation))
  }

  def ParamType = rule { K.O("=>") ~ Type | Type ~ "*" | Type }

  def Expr: R0 = rule {
    (Bindings | optional(K.W("implicit")) ~ Id | "_") ~ K.O("=>") ~ Expr |
    Expr1
  }
  def Expr1: R0 = rule {
    IfCFlow |
    WhileCFlow |
    TryCFlow |
    DoWhileCFlow |
    ForCFlow |
    K.W("throw") ~ Expr |
    K.W("return") ~ optional(Expr) |
    SimpleExpr ~ K.O("=") ~ Expr |
    PostfixExpr ~ optional("match" ~ '{' ~ CaseClauses ~ "}" | Ascription)

  }
  def IfCFlow = rule { "if" ~ '(' ~ Expr ~ ')' ~ zeroOrMore(Newline) ~ Expr ~ optional(optional(Semi) ~ K.W("else") ~ Expr) }
  def WhileCFlow = rule { "while" ~ '(' ~ Expr ~ ')' ~ zeroOrMore(Newline) ~ Expr }
  def TryCFlow = rule {
    K.W("try") ~ Expr ~
      optional(WL ~ K.W("catch") ~ Expr) ~
      optional(WL ~ K.W("finally") ~ Expr)
  }

  def DoWhileCFlow = rule { K.W("do") ~ Expr ~ optional(Semi) ~ "while" ~ '(' ~ Expr ~ ")" }
  def ForCFlow = rule {
    "for" ~
      ('(' ~ Enumerators ~ ')' | '{' ~ Enumerators ~ '}') ~
      zeroOrMore(Newline) ~
      optional(K.W("yield")) ~
      Expr }
  def NotNewline: R0 = rule{ &( WS ~ noneOf("\n") )}
  def PostfixExpr: R0 = rule { InfixExpr ~ optional(NotNewline ~ Id ~ optional(Newline)) }
  def InfixExpr: R0 = rule {
    PrefixExpr ~
    zeroOrMore(
      NotNewline ~
      Id ~
      optional(Newline) ~
      PrefixExpr
    )
  }
  def PrefixExpr = rule { optional(WL ~ anyOf("-+~!")) ~ SimpleExpr }

  def SimpleExpr: R0 = rule {
    SimpleExpr1 ~
      zeroOrMore(WL ~ ('.' ~ Id | TypeArgs | ArgumentExprs)) ~
      optional(WL ~ "_")
  }

  def SimpleExpr1 = rule{
    K.W("new") ~ (ClassTemplate | TemplateBody) |
      BlockExpr |
      Literal |
      Path |
      K.W("_") |
      '(' ~ optional(Exprs) ~ ")"
  }



  def Exprs: R0 = rule { oneOrMore(Expr).separatedBy(',') }
  def ArgumentExprs: R0 = rule {
    '(' ~ optional(Exprs ~ optional(K.O(":") ~ K.W("_") ~ '*')) ~ ")" |
      optional(Newline) ~ BlockExpr
  }

  def BlockExpr: R0 = rule { '{' ~ (CaseClauses | Block) ~ "}" }
  def BlockEnd: R0 = rule{ optional(Semis) ~ &("}" | "case") }
  def Block: R0 = rule {
     optional(Semis) ~
     (
       BlockStats ~ optional(Semis ~ ResultExpr) ~ BlockEnd |
       ResultExpr ~ BlockEnd |
       MATCH ~ BlockEnd
     )
  }
  def BlockStats: R0 = rule{
    oneOrMore(BlockStat).separatedBy(Semis)
  }
  def BlockStat: R0 = rule {
    Import |
    zeroOrMore(Annotation) ~ (optional(K.W("implicit") | K.W("lazy")) ~ Def | zeroOrMore(LocalModifier) ~ TmplDef) |
    Expr1
  }
  def ResultExpr: R0 = rule {
    (Bindings | optional(K.W("implicit")) ~ Id | "_") ~ K.W("=>") ~ Block | Expr1
  }
  def Enumerators: R0 = rule { Generator ~ zeroOrMore(Semi ~ Enumerator) ~ WL }
  def Enumerator: R0 = rule { Generator | Guard | Pattern1 ~ K.O("=") ~ Expr }
  def Generator: R0 = rule { Pattern1 ~ K.O("<-") ~ Expr ~ optional(WL ~ Guard)  }
  def CaseClauses: R0 = rule { oneOrMore(CaseClause) }
  def CaseClause: R0 = rule { K.W("case") ~ Pattern ~ optional(Guard) ~ K.O("=>") ~ Block }
  def Guard: R0 = rule { K.W("if") ~ PostfixExpr }
  def Pattern: R0 = rule {
    oneOrMore(Pattern1).separatedBy('|')
  }
  def Pattern1: R0 = rule {
    K.W("_") ~ K.O(":") ~ TypePat | VarId ~ K.O(":") ~ TypePat | Pattern2
  }
  def Pattern2: R0 = rule {
    VarId ~ "@" ~ Pattern3 | Pattern3 | VarId
  }
  def Pattern3: R0 = rule {
    SimplePattern ~ zeroOrMore(Id ~ SimplePattern)
  }
  def SimplePattern: R0 = rule {
    K.W("_") |
    Literal |
    '(' ~ optional(Patterns) ~ ')' |
    (
      StableId ~
      optional(
        '(' ~
          (optional(Patterns ~ ',') ~ optional(VarId ~ '@') ~ K.W("_") ~ '*' | optional(Patterns)) ~
          ')'
      )
    ) |
    VarId
  }
  def Patterns: R0 = rule { K.W("_") ~ '*' | oneOrMore(Pattern).separatedBy(',') }

  def TypeParamClause: R0 = rule { '[' ~ oneOrMore(VariantTypeParam).separatedBy(',') ~ ']' }
  def FunTypeParamClause: R0 = rule { '[' ~ oneOrMore(TypeParam).separatedBy(',') ~ ']' }
  def VariantTypeParam: R0 = rule { zeroOrMore(Annotation) ~ optional(anyOf("+-")) ~ TypeParam }
  def TypeParam: R0 = rule {
    (Id | K.W("_")) ~
      optional(TypeParamClause) ~
      optional(K.O(">:") ~ Type) ~
      optional(K.O("<:") ~ Type) ~
      zeroOrMore(K.O("<%") ~ Type) ~
      zeroOrMore(K.O(":") ~ Type)
  }
  def ParamClauses: R0 = rule { zeroOrMore(ParamClause) ~ optional(optional(Newline) ~ '(' ~ K.W("implicit") ~ Params ~ ')') }
  def ParamClause: R0 = rule { optional(Newline) ~ '(' ~ optional(Params) ~ ')' }
  def Params: R0 = rule { zeroOrMore(Param).separatedBy(',') }
  def Param: R0 = rule { zeroOrMore(Annotation) ~ Id ~ optional(K.O(":") ~ ParamType) ~ optional(K.O("=") ~ Expr) }
  def ClassParamClauses: R0 = rule { zeroOrMore(ClassParamClause) ~ optional(optional(Newline) ~ '(' ~ K.W("implicit") ~ ClassParam ~ ")") }
  def ClassParamClause: R0 = rule { optional(Newline) ~ '(' ~ optional(ClassParams) ~ ")" }
  def ClassParams: R0 = rule { oneOrMore(ClassParam).separatedBy(',') }
  def ClassParam: R0 = rule { zeroOrMore(Annotation) ~ optional(zeroOrMore(Modifier) ~ (K.W("val") | K.W("var"))) ~ Id ~ K.O(":") ~ ParamType ~ optional(K.O("=") ~ Expr) }

  def Bindings: R0 = rule { '(' ~ zeroOrMore(Binding).separatedBy(',') ~ ')' }
  def Binding: R0 = rule { (Id | K.W("_")) ~ optional(K.O(":") ~ Type) }

  def Modifier: R0 = rule { LocalModifier | AccessModifier | K.W("override") }
  def LocalModifier: R0 = rule { K.W("abstract") | K.W("final") | K.W("sealed") | K.W("implicit") | K.W("lazy") }
  def AccessModifier: R0 = rule { (K.W("private") | K.W("protected")) ~ optional(AccessQualifier) }
  def AccessQualifier: R0 = rule { '[' ~ (K.W("this") | Id) ~ ']' }

  def Annotation: R0 = rule { '@' ~ SimpleType ~ zeroOrMore(WL ~ ArgumentExprs) }
  def ConstrAnnotation: R0 = rule { '@' ~ SimpleType ~ ArgumentExprs }

  def TemplateBody: R0 = rule {
    '{' ~
    optional(SelfType) ~
    zeroOrMore(TemplateStat).separatedBy(Semis) ~
    '}'
  }
  def TemplateStat: R0 = rule {
    Import |
    zeroOrMore(Annotation ~ optional(Newline)) ~ zeroOrMore(Modifier) ~ (Def | Dcl) |
    Expr
  }

  def SelfType: R0 = rule { K.W("this") ~ K.O(":") ~ Type ~ K.O("=>") | Id ~ optional(K.O(":") ~ Type) ~ K.O("=>") }

  def Import: R0 = rule { K.W("import") ~ oneOrMore(ImportExpr).separatedBy(',') }

  def ImportExpr: R0 = rule {
    StableId ~ optional('.' ~ ("_" | ImportSelectors))
  }
  def ImportSelectors: R0 = rule { '{' ~ zeroOrMore(ImportSelector ~ ',') ~ (ImportSelector | K.W("_")) ~ "}" }
  def ImportSelector: R0 = rule { Id ~ optional(K.O("=>") ~ (Id | K.W("_"))) }

  def Dcl: R0 = rule {
    K.W("val") ~ ValDcl |
      K.W("var") ~ VarDcl |
      K.W("def") ~ FunDcl |
      K.W("type") ~ zeroOrMore(Newline) ~ TypeDcl
  }
  def ValDcl: R0 = rule { Ids ~ K.O(":") ~ Type }
  def VarDcl: R0 = rule { Ids ~ K.O(":") ~ Type }
  def FunDcl: R0 = rule { FunSig ~ optional(WL ~ K.O(":") ~ Type) }
  def FunSig: R0 = rule { Id ~ optional(FunTypeParamClause) ~ ParamClauses }
  def TypeDcl: R0 = rule {
    Id ~
      optional(WL ~ TypeParamClause) ~
      optional(WL ~ K.O(">:") ~ Type) ~
      optional(WL ~ K.O("<:") ~ Type)
  }

  def PatVarDef: R0 = rule { K.W("val") ~ PatDef | K.W("var") ~ VarDef }
  def Def: R0 = rule { K.W("def") ~ FunDef | K.W("type") ~ zeroOrMore(Newline) ~ TypeDef | PatVarDef | TmplDef }
  def PatDef: R0 = rule { oneOrMore(Pattern2).separatedBy(',') ~ optional(K.O(":") ~ Type) ~ K.O("=") ~ Expr }
  def VarDef: R0 = rule { Ids ~ K.O(":") ~ Type ~ K.O("=") ~ K.W("_") | PatDef }
  def FunDef: R0 = rule {
    K.W("this") ~ ParamClause ~ ParamClauses ~ (K.O("=") ~ ConstrExpr | optional(Newline) ~ ConstrBlock) |
      FunSig ~
        (
          optional(K.O(":") ~ Type) ~ K.O("=") ~ optional(K.W("macro")) ~ Expr |
            optional(Newline) ~ '{' ~ Block ~ "}"
          )
  }
  def TypeDef: R0 = rule { Id ~ optional(TypeParamClause) ~ K.O("=") ~ Type }

  def TmplDef: R0 = rule {
    K.W("trait") ~ TraitDef |
    optional(K.W("case")) ~ (K.W("class") ~ ClassDef |
    K.W("object") ~ ObjectDef)
  }
  def ClassDef: R0 = rule {
    Id ~
      optional(TypeParamClause) ~
      zeroOrMore(ConstrAnnotation) ~
      optional(AccessModifier) ~
      ClassParamClauses ~
      ClassTemplateOpt
  }
  def TraitDef: R0 = rule { Id ~ optional(TypeParamClause) ~ TraitTemplateOpt }
  def ObjectDef: R0 = rule { Id ~ ClassTemplateOpt }
  def ClassTemplateOpt: R0 = rule {
    WL ~ K.W("extends") ~ ClassTemplate |
      optional(WL ~ optional(K.W("extends")) ~ TemplateBody)
  }
  def TraitTemplateOpt: R0 = rule { K.W("extends") ~ TraitTemplate | optional(optional(K.W("extends")) ~ TemplateBody) }
  def ClassTemplate: R0 = rule {
    optional(EarlyDefs) ~
      ClassParents ~
      optional(WL ~ TemplateBody)
  }

  def TraitTemplate: R0 = rule {
    optional(EarlyDefs) ~ TraitParents ~ optional(TemplateBody)
  }
  def ClassParents: R0 = rule {
    Constr ~ zeroOrMore(WL ~ K.W("with") ~ AnnotType)
  }
  def TraitParents: R0 = rule {
    AnnotType ~ zeroOrMore(WL ~ K.W("with") ~ AnnotType)
  }
  def Constr: R0 = rule {
    AnnotType ~ zeroOrMore(WL ~ ArgumentExprs)
  }
  def EarlyDefs: R0 = rule {
    '{' ~ optional(oneOrMore(EarlyDef).separatedBy(Semis)) ~ '}' ~ K.W("with")
  }
  def EarlyDef: R0 = rule {
    zeroOrMore(Annotation ~ optional(Newline)) ~ zeroOrMore(Modifier) ~ PatVarDef
  }
  def ConstrExpr: R0 = rule { ConstrBlock | SelfInvocation }
  def ConstrBlock: R0 = rule { '{' ~ SelfInvocation ~ zeroOrMore(Semis ~ BlockStat) ~ '}' }
  def SelfInvocation: R0 = rule { K.W("this") ~ oneOrMore(ArgumentExprs) }

  def TopStatSeq: R0 = rule { oneOrMore(TopStat).separatedBy(Semis) }
  def TopStat: R0 = rule {
    Packaging |
      PackageObject |
      Import |
      zeroOrMore(Annotation ~ optional(Newline)) ~ zeroOrMore(Modifier) ~ TmplDef
  }
  def Packaging: R0 = rule { K.W("package") ~ QualId ~ '{' ~ TopStatSeq ~ '}' }
  def PackageObject: R0 = rule { K.W("package") ~ K.W("object") ~ ObjectDef }
  def TopPackageSeq: R0 = rule{
    oneOrMore(K.W("package") ~ QualId).separatedBy(Semis)
  }
  def CompilationUnit: Rule1[String] = rule {
    capture(
      pr("CompulationUnit 0") ~
      optional(Semis) ~
      pr("CompulationUnit 1") ~
      (TopPackageSeq ~ optional(Semis ~ TopStatSeq) | TopStatSeq) ~
      optional(Semis) ~
      WL

    )
  }
}
