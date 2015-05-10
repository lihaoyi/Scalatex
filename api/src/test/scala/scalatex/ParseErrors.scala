package scalatex

import utest._

import scalatex.Internals.{DebugFailure, twRuntimeErrors, twfRuntimeErrors}

/**
* Created by haoyi on 7/14/14.
*/
object ParseErrors extends TestSuite{
  def check(input: String, expectedTrace: String) = {

    val failure = new stages.Parser()
      .Body
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
      """@a.b.""
        |""".stripMargin,
      """(BacktickId | PlainId):5 ..."\"\"\n""""
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
      """"""
    )
  }
}
