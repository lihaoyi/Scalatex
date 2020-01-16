package scalatex

import utest._
import fastparse._
import scalatex.Internals.{DebugFailure, twRuntimeErrors, twfRuntimeErrors}

/**
* Created by haoyi on 7/14/14.
*/
object ParseErrors extends TestSuite {

  def check(input: String, expectedTrace: String) = {
    val failure = parse(input, new stages.Parser().File(_)).asInstanceOf[fastparse.Parsed.Failure]
    val parsedTrace = failure.trace().msg //trace(false)
    assert(parsedTrace.trim == expectedTrace.trim)
  }

  val tests = Tests {
    * - check(
      """@{)
        |""".stripMargin,
      """ Expected "}":1:3, found ")\n" """
    )
    * - check(
      """@({
        |""".stripMargin,
      """ Expected "}":2:1, found "" """
    )
    * - check(
      """@for{;
        |""".stripMargin,
      """ Expected (TypePattern | BindPattern):1:6, found ";\n" """
    )
    * - check(
      """@{ => x
        |""".stripMargin,
      """ Expected "}":1:4, found "=> x\n" """
    )

    * - check(
      """@( => x
        |""".stripMargin,
      """ Expected ")":1:4, found "=> x\n" """
    )
    * - check(
      """@x{
        |""".stripMargin,
      """ Expected "}":2:1, found "" """
    )
    * - check(
      """@ """.stripMargin,
      """ Expected (IndentForLoop | IndentScalaChain | IndentIfElse | HeaderBlock | @@):1:2, found " """"
    )

    * - check(
      """@p
        |  @if(
        |""".stripMargin,
      """ Expected (If | While | Try | DoWhile | For | Throw | Return | ImplicitLambda | SmallerExprOrLambda):2:7, found "\n" """
    )
    * - check(
      """@if(true){ 123 }else lol
        |""".stripMargin,
      """ Expected "{":1:21, found " lol\n" """
    )
  }
}
