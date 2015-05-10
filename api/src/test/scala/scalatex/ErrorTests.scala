package scalatex

import utest._
import scalatex.stages._
import scalatags.Text.all._
import scalatex.Internals.{DebugFailure, twRuntimeErrors, twfRuntimeErrors}

/**
* Created by haoyi on 7/14/14.
*/
object ErrorTests extends TestSuite{
  def check(x: => Unit, expectedMsg: String, expectedError: String) = {
    val DebugFailure(msg, pos) = intercept[DebugFailure](x)
    def format(str: String) = {
      val whitespace = " \t\n".toSet
      "\n" + str.dropWhile(_ == '\n')
                .reverse
                .dropWhile(whitespace.contains)
                .reverse
    }
    // Format these guys nicely to normalize them and make them
    // display nicely in the assert error message if it blows up
    val formattedPos = format(pos)
    val formattedExpectedPos = format(expectedError)

    assert(msg.contains(expectedMsg))
    assert(formattedPos == formattedExpectedPos)
  }

  val tests = TestSuite{
    'simple {
      * - check(
        twRuntimeErrors("@o"),
        """not found: value o""",
        """
        twRuntimeErrors("@o"),
                          ^
        """
      )
      val tq = "\"\"\""
      * - check(

        twRuntimeErrors("""@o"""),
        """not found: value o""",
        s"""
        twRuntimeErrors($tq@o$tq),
                            ^
        """
      )

      * - check(
        twRuntimeErrors("omg @notInScope lol"),
        """not found: value notInScope""",
        """
        twRuntimeErrors("omg @notInScope lol"),
                              ^
        """
      )
    }
    'chained{
      'properties {
        * - check(
          twRuntimeErrors("omg @math.lol lol"),
          """object lol is not a member of package math""",
          """
          twRuntimeErrors("omg @math.lol lol"),
                                    ^
          """
        )

        * - check(
          twRuntimeErrors("omg @math.E.lol lol"),
          """value lol is not a member of Double""",
          """
          twRuntimeErrors("omg @math.E.lol lol"),
                                      ^
          """
        )
        * - check(
          twRuntimeErrors("omg @_root_.scala.math.lol lol"),
          """object lol is not a member of package math""",
          """
          twRuntimeErrors("omg @_root_.scala.math.lol lol"),
                                                 ^
          """
        )
        * - check(
          twRuntimeErrors("omg @_root_.scala.gg.lol lol"),
          """object gg is not a member of package scala""",
          """
          twRuntimeErrors("omg @_root_.scala.gg.lol lol"),
                                            ^
          """
        )
        * - check(
          twRuntimeErrors("omg @_root_.ggnore.math.lol lol"),
          """object ggnore is not a member of package <root>""",
          """
          twRuntimeErrors("omg @_root_.ggnore.math.lol lol"),
                                      ^
          """
        )
      }
      'calls{
        * - check(
          twRuntimeErrors("@scala.QQ.abs(-10).tdo(10).sum.z"),
          """object QQ is not a member of package scala""",
          """
          twRuntimeErrors("@scala.QQ.abs(-10).tdo(10).sum.z"),
                                 ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.math.abs(-10).tdo(10).sum.z"),
          "value tdo is not a member of Int",
          """
          twRuntimeErrors("@scala.math.abs(-10).tdo(10).sum.z"),
                                               ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.math.abs(-10).to(10).sum.z"),
          "value z is not a member of Int",
          """
          twRuntimeErrors("@scala.math.abs(-10).to(10).sum.z"),
                                                          ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.math.abs(-10).to(10).sum.z()"),
          "value z is not a member of Int",
          """
          twRuntimeErrors("@scala.math.abs(-10).to(10).sum.z()"),
                                                          ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.math.abs(-10).cow.sum.z"),
          "value cow is not a member of Int",
          """
          twRuntimeErrors("@scala.math.abs(-10).cow.sum.z"),
                                               ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.smath.abs.cow.sum.z"),
          "object smath is not a member of package scala",
          """
          twRuntimeErrors("@scala.smath.abs.cow.sum.z"),
                                 ^
          """
        )
        * - check(
          twRuntimeErrors("@scala.math.cos('omg)"),
          "type mismatch",
          """
          twRuntimeErrors("@scala.math.cos('omg)"),
                                           ^
          """
        )

        * - check(
          twRuntimeErrors("@scala.math.cos[omg]('omg)"),
          "not found: type omg",
          """
          twRuntimeErrors("@scala.math.cos[omg]('omg)"),
                                           ^
          """
        )
        * - check(
          twRuntimeErrors("""
            I am cow hear me moo
            @scala.math.abs(-10).tdo(10).sum.z
            I weigh twice as much as you
          """),
          "value tdo is not a member of Int",
          """
            @scala.math.abs(-10).tdo(10).sum.z
                                ^
          """
        )
      }
      'curlies{
        * - check(
          twRuntimeErrors("@p{@Seq(1, 2, 3).foldLeft(0)}"),
          "missing arguments for method foldLeft",
          """
          twRuntimeErrors("@p{@Seq(1, 2, 3).foldLeft(0)}"),
                                                    ^
          """
        )

        * - check(
          twRuntimeErrors("@Nil.foldLeft{XY}"),
          "missing arguments for method foldLeft",
          """
          twRuntimeErrors("@Nil.foldLeft{XY}"),
                                        ^
          """
        )

        * - check(
          twRuntimeErrors("@Seq(1).map{(y: String) => omg}"),
            "type mismatch",
            """
          twRuntimeErrors("@Seq(1).map{(y: String) => omg}"),
                                       ^
          """
        )
        * - check(
          twRuntimeErrors("@Nil.map{ omg}"),
          "too many arguments for method map",
          """
          twRuntimeErrors("@Nil.map{ omg}"),
                                    ^
          """
        )
      }
      'callContents{
        * - check(
          twRuntimeErrors("@scala.math.abs((1, 2).wtf)"),
          "value wtf is not a member of (Int, Int)",
          """
          twRuntimeErrors("@scala.math.abs((1, 2).wtf)"),
                                                  ^
          """
        )

        * - check(
          twRuntimeErrors("@scala.math.abs((1, 2).swap._1.toString().map(_.toString.wtf))"),
          "value wtf is not a member of String",
          """
          twRuntimeErrors("@scala.math.abs((1, 2).swap._1.toString().map(_.toString.wtf))"),
                                                                                    ^
          """
        )
      }
    }
    'ifElse{
      'oneLine {
        * - check(
          twRuntimeErrors("@if(math > 10){ 1 }else{ 2 }"),
          "object > is not a member of package math",
          """
          twRuntimeErrors("@if(math > 10){ 1 }else{ 2 }"),
                                    ^
          """
        )
        * - check(
          twRuntimeErrors("@if(true){ (@math.pow(10)) * 10  }else{ 2 }"),
          "Unspecified value parameter y",
          """
          twRuntimeErrors("@if(true){ (@math.pow(10)) * 10  }else{ 2 }"),
                                                ^
          """
        )
        * - check(
          twRuntimeErrors("@if(true){ * 10  }else{ @math.sin(3, 4, 5) }"),
          "too many arguments for method sin: (x: Double)Double",
          """
          twRuntimeErrors("@if(true){ * 10  }else{ @math.sin(3, 4, 5) }"),
                                                            ^
          """
        )
      }
      'multiLine{
        * - check(
          twRuntimeErrors("""
            Ho Ho Ho

            @if(math != 10)
              I am a cow
            @else
              You are a cow
            GG
          """),
          "object != is not a member of package math",
          """
            @if(math != 10)
                     ^
          """
        )
        * - check(
          twRuntimeErrors("""
            Ho Ho Ho

            @if(4 != 10)
              I am a cow @math.lols
            @else
              You are a cow
            GG
          """),
          "object lols is not a member of package math",
          """
              I am a cow @math.lols
                              ^
          """
        )
        * - check(
          twRuntimeErrors("""
            Ho Ho Ho

            @if(12 != 10)
              I am a cow
            @else
              @math.E.toString.gog(1)
            GG
          """),
          "value gog is not a member of String",
          """
              @math.E.toString.gog(1)
                              ^
          """
        )
      }
    }
    'forLoop{
      'oneLine{
        'header - check(
          twRuntimeErrors("omg @for(x <- (0 + 1 + 2) omglolol (10 + 11 + 2)){ hello }"),
          """value omglolol is not a member of Int""",
          """
          twRuntimeErrors("omg @for(x <- (0 + 1 + 2) omglolol (10 + 11 + 2)){ hello }"),
                                                     ^
          """
        )

        'body - check(
          twRuntimeErrors("omg @for(x <- 0 until 10){ @((x, 2) + (1, 2)) }"),
          """too many arguments for method +""",
          """
          twRuntimeErrors("omg @for(x <- 0 until 10){ @((x, 2) + (1, 2)) }"),
                                                              ^
          """
        )
      }
      'multiLine{
        'body - check(
          twRuntimeErrors("""
            omg
            @for(x <- 0 until 10)
              I am cow hear me moo
              I weigh twice as much as @x.kkk
          """),
          """value kkk is not a member of Int""",
          """
              I weigh twice as much as @x.kkk
                                         ^
          """
        )
      }
    }
    'multiLine{
      'missingVar - check(
        twRuntimeErrors("""
        omg @notInScope lol
        """),
        """not found: value notInScope""",
        """
        omg @notInScope lol
            ^
        """
      )
//      'wrongType - check(
//        twRuntimeErrors("""
//        omg @{() => ()} lol
//        """),
//        """type mismatch""",
//        """
//        omg @{() => ()} lol
//                 ^
//        """
//      )

      'bigExpression - check(
        twRuntimeErrors("""
          @{
            val x = 1 + 2
            val y = new Object()
            val z = y * x
            x
          }
        """),
        "value * is not a member of Object",
        """
            val z = y * x
                      ^
        """
      )
      'blocks - check(
        twRuntimeErrors("""
          lol
          omg
          wtf
          bbq
          @body
            @div
              @span
                @lol
        """),
        "not found: value lol",
        """
                @lol
                 ^
        """
      )
    }
    'files{
      * - check(
        twfRuntimeErrors("api/src/test/resources/scalatex/errors/Simple.scalatex"),
        "not found: value kk",
        """
          |Hello @kk
          |        ^
        """.stripMargin
      )

      * - check(
        twfRuntimeErrors("api/src/test/resources/scalatex/errors/Nested.scalatex"),
        "not found: value y",
        """
          |            @y
          |              ^
        """.stripMargin
      )
    }
  }
}
