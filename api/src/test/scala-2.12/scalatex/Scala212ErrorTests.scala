package scalatex

import utest._
import scalatex.stages._
import scalatags.Text.all._
import scalatex.Internals.{DebugFailure, twRuntimeErrors, twfRuntimeErrors}
import ErrorTests.check

object Scala212ErrorTests extends TestSuite {
  val tests = TestSuite {
    'chained {
      'curlies {
        * - check(
          twRuntimeErrors("@p{@Seq(1, 2, 3).foldLeft(0)}"),
          "type mismatch",
          """
          twRuntimeErrors("@p{@Seq(1, 2, 3).foldLeft(0)}"),
                                                    ^
          """
        )
        * - check(
          twRuntimeErrors("@Nil.map{ @omg}"),
          "too many arguments (2) for method map: ",
          """
          twRuntimeErrors("@Nil.map{ @omg}"),
                                      ^
          """
        )
      }
    }
    'ifElse {
      'oneLine {
        * - check(
          twRuntimeErrors("@if(true){ * 10  }else{ @math.sin(3, 4, 5) }"),
          "too many arguments (3) for method sin: (x: Double)Double",
          """
          twRuntimeErrors("@if(true){ * 10  }else{ @math.sin(3, 4, 5) }"),
                                                                ^
          """
        )
      }
    }
    'forLoop {
      'oneLine {
        'body - check(
          twRuntimeErrors("omg @for(x <- 0 until 10){ @((x, 2) + (1, 2)) }"),
          """too many arguments (2) for method +: (other: String)String""",
          """
          twRuntimeErrors("omg @for(x <- 0 until 10){ @((x, 2) + (1, 2)) }"),
                                                                     ^
          """
        )
      }
    }
  }
}