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
      .asInstanceOf[fastparse.core.Result.Failure]

    val parsedTrace = failure.traced.copy(fullStack = Nil).trace
    assert(parsedTrace == expectedTrace.trim)
  }

  val tests = TestSuite {
    * - check(
      """@{)
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):2 ...")\n" """
    )
    * - check(
      """@({
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):4 ..."" """
    )
    * - check(
      """@for{;
        |""".stripMargin,
      """(TypePattern | BindPattern):5 ...";\n""""
    )
    * - check(
      """@{ => x
        |""".stripMargin,
      """ (";" | Newline.rep(1) | "}" | `case`):3 ..."=> x\n" """
    )

    * - check(
      """@( => x
        |""".stripMargin,
      """ (If | While | Try | DoWhile | For | Throw | Return | ImplicitLambda | SmallerExprOrLambda | ")"):3 ..."=> x\n" """
    )
    * - check(
      """@x{
        |""".stripMargin,
      """ ("}" | IndentedExpr | "@" ~! CtrlFlow | BodyText):4 ..."" """
    )
    * - check(
      """@ """.stripMargin,
      """(IndentForLoop | IndentScalaChain | IndentIfElse | HeaderBlock | @@):1 ..." """"
    )

    * - check(
      """@p
        |  @if(
        |""".stripMargin,
      """(If | While | Try | DoWhile | For | Throw | Return | ImplicitLambda | SmallerExprOrLambda):9 ..."\n""""
    )
    * - check(
      """@if(true){ 123 }else lol
        |""".stripMargin,
      """"{":20 ..." lol\n""""
    )
  }
}
