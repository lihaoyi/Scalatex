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
      .parse(input, trace=false)
      .asInstanceOf[fastparse.Result.Failure]

    val parsedTrace = failure.trace
    assert(parsedTrace == expectedTrace)
  }

  val tests = TestSuite {
    * - check(
      """@{)
        |""".stripMargin,
      """("}" | `case`):2 ...")\n""""
    )
    * - check(
      """@({
        |""".stripMargin,
      """("}" | `case`):4 ..."""""
    )
    * - check(
      """@for{;
        |""".stripMargin,
      """(TypePattern | BindPattern):5 ...";\n""""
    )
    * - check(
      """@{ => x
        |""".stripMargin,
      """("}" | `case`):2 ..." => x\n""""
    )

    * - check(
      """@( => x
        |""".stripMargin,
      """")":3 ..."=> x\n""""
    )
    * - check(
      """@x{
        |""".stripMargin,
      """"}":4 ..."""""
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
