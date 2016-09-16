package scalatex

import utest._

import scalatex.Internals.{DebugFailure, twRuntimeErrors, twfRuntimeErrors}

/**
* Created by haoyi on 7/14/14.
*/
object ParseErrors extends TestSuite{
  def check(input: String, expectedTrace: String) = {

    val failure = new stages.Parser()
      .File
      .parse(input)
      .asInstanceOf[fastparse.all.Parsed.Failure]

    val parsedTrace = failure.extra.traced.copy(fullStack = Vector.empty).trace
    assert(parsedTrace == expectedTrace.trim)
  }

  val tests = TestSuite {
    * - check(
      """@{)
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):1:3 ...")\n" """
    )
    * - check(
      """@({
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):2:1 ..."" """
    )
    * - check(
      """@for{;
        |""".stripMargin,
      """(TypePattern | BindPattern):1:6 ...";\n""""
    )
    * - check(
      """@{ => x
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):1:4 ..."=> x\n" """
    )

    * - check(
      """@( => x
        |""".stripMargin,
      """ (If | While | Try | DoWhile | For | Throw | Return | ImplicitLambda | SmallerExprOrLambda | ")"):1:4 ..."=> x\n" """
    )
    * - check(
      """@x{
        |""".stripMargin,
      """ ("}" | IndentedExpr | "@" ~/ CtrlFlow | BodyText):2:1 ..."" """
    )
    * - check(
      """@ """.stripMargin,
      """(IndentForLoop | IndentScalaChain | IndentIfElse | HeaderBlock | @@):1:2 ..." """"
    )

    * - check(
      """@p
        |  @if(
        |""".stripMargin,
      """(If | While | Try | DoWhile | For | Throw | Return | ImplicitLambda | SmallerExprOrLambda):2:7 ..."\n""""
    )
    * - check(
      """@if(true){ 123 }else lol
        |""".stripMargin,
      """"{":1:21 ..." lol\n""""
    )
  }
}
