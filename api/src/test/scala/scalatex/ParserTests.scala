package scalatex


import org.parboiled2._
import scalaParser.ScalaSyntax

import scalatex.stages.{Trim, Parser, Ast}
import scalatex.stages.Ast.Block.{IfElse, For, Text}
import Ast.Chain.Args

object ParserTests extends utest.TestSuite{
  import Ast._
  import utest._
  def check[T](input: String, parse: Parser => scala.util.Try[T], expected: T) = {
    val parsed = parse(new Parser(input)).get
    assert(parsed == expected)
  }
  def tests = TestSuite{
    'Trim{
      def wrap(s: String) = "|" + s + "|"
      * - {
        val trimmed = wrap(stages.Trim.old("""
            i am cow
              hear me moo
                i weigh twice as much as you
        """))
        val expected = wrap("""
                         |i am cow
                         |  hear me moo
                         |    i weigh twice as much as you
                         |""".stripMargin)
         assert(trimmed == expected)

      }
      * - {
        val trimmed = wrap(stages.Trim.old(
          """
          @{"lol" * 3}
          @{
            val omg = "omg"
            omg * 2
          }
          """
        ))
        val expected = wrap(
          """
            |@{"lol" * 3}
            |@{
            |  val omg = "omg"
            |  omg * 2
            |}
            |""".stripMargin
        )
        assert(trimmed == expected)
      }
      'dropTrailingWhitespace - {

        val trimmed = wrap(stages.Trim.old(
          Seq(
            "  i am a cow   ",
            "    hear me moo    ",
            "   i weigh twice as much as you"
          ).mkString("\n")
        ))
        val expected = wrap(
          Seq(
            "i am a cow",
            "  hear me moo",
            " i weigh twice as much as you"
          ).mkString("\n")
        )
        assert(trimmed == expected)
      }
    }
    'Text {
      * - check("i am a cow", _.Text.run(), Block.Text(0, "i am a cow"))
      * - check("i am a @cow", _.Text.run(), Block.Text(0, "i am a "))
      * - check("i am a @@cow", _.Text.run(), Block.Text(0, "i am a @cow"))
      * - check("i am a @@@cow", _.Text.run(), Block.Text(0, "i am a @"))
      * - check("i am a @@@@cow", _.Text.run(), Block.Text(0, "i am a @@cow"))

    }
    'Code{
      'identifier - check("@gggg  ", _.Code.run(), "gggg")
      'parens - check("@(1 + 1)lolsss\n", _.Code.run(),  "(1 + 1)")
      'curlies - check("@{{1} + (1)}  ", _.Code.run(), "{{1} + (1)}")
      'blocks - check("@{val x = 1; 1} ", _.Code.run(), "{val x = 1; 1}")
      'weirdBackticks - check("@{`{}}{()@`}\n", _.Code.run(), "{`{}}{()@`}")
     }
    'MiscCode{
      'imports{
        * - check("@import math.abs", _.Header.run(), "import math.abs")
        * - check("@import math.{abs, sin}", _.Header.run(), "import math.{abs, sin}")
      }
      'headerblocks{
        check(
          """@import math.abs
            |@import math.sin
            |
            |hello world
            |""".stripMargin,
          _.HeaderBlock.run(),
          Ast.Header(
            0,
            "import math.abs\nimport math.sin",
            Ast.Block(33,
              Seq(Text(33, "\n\nhello world\n"))
            )
          )
        )
      }
      'caseclass{
        check(
          """@case class Foo(i: Int, s: String)
          """.stripMargin,
          _.Header.run(),
          "case class Foo(i: Int, s: String)"
        )
      }

    }
    'Block{
      * - check("{i am a cow}", _.BraceBlock.run(), Block(1, Seq(Block.Text(1, "i am a cow"))))
      * - check("{i @am a @cow}", _.BraceBlock.run(),
        Block(1, Seq(
          Block.Text(1, "i "),
          Chain(3, "am",Seq()),
          Block.Text(6, " a "),
          Chain(9, "cow",Seq())
        ))
      )
    }
    'Chain{
      * - check("@omg.bbq[omg].fff[fff](123)  ", _.ScalaChain.run(),
        Chain(0, "omg",Seq(
          Chain.Prop(4, "bbq"),
          Chain.TypeArgs(8, "[omg]"),
          Chain.Prop(13, "fff"),
          Chain.TypeArgs(17, "[fff]"),
          Chain.Args(22, "(123)")
        ))
      )
      * - check("@omg{bbq}.cow(moo){a @b}\n", _.ScalaChain.run(),
        Chain(0, "omg",Seq(
          Block(5, Seq(Text(5, "bbq"))),
          Chain.Prop(9, "cow"),
          Chain.Args(13, "(moo)"),
          Block(19, Seq(Text(19, "a "), Chain(21, "b", Nil)))
        ))
      )
    }
    'ControlFlow{
      'for {
        'for - check(
          "@for(x <- 0 until 3){lol}",
          _.ForLoop.run(),
          For(0, "for(x <- 0 until 3)", Block(21, Seq(Text(21, "lol"))))
        )
        'forBlock - check(
          """
            |@for(x <- 0 until 3)
            |  lol""".stripMargin,
          _.Body.run(),
          Block(0, Seq(
            Text(0, "\n"),
            For(
              1,
              "for(x <- 0 until 3)",
              Block(21, Seq(Text(21, "\n  lol")))
            )
          ))
        )
        'forBlockBraces - check(
          """
            |@for(x <- 0 until 3){
            |  lol
            |}""".stripMargin,
          _.Body.run(),
          Block(0, Seq(
            Text(0, "\n"),
            For(
              1,
              "for(x <- 0 until 3)",
              Block(22, Seq(Text(22, "\n  lol\n")))
            )
          ))
        )
      }
      'ifElse {
        'if - check(
          "@if(true){lol}",
          _.IfElse.run(),
          IfElse(0, "if(true)", Block(10, Seq(Text(10, "lol"))), None)
        )
        'ifElse - check(
          "@if(true){lol}else{ omg }",
          _.IfElse.run(),
          IfElse(0, "if(true)", Block(10, Seq(Text(10, "lol"))), Some(Block(19, Seq(Text(19, " omg ")))))
        )
        'ifBlock - check(
          """
            |@if(true)
            |  omg""".stripMargin,
          _.IfElse.run(),
          IfElse(1, "if(true)", Block(10, Seq(Text(10, "\n  omg"))), None)
        )
        'ifBlockElseBlock - check(
          """
            |@if(true)
            |  omg
            |@else
            |  wtf""".stripMargin,
          _.IfElse.run(),
          IfElse(
            1,
            "if(true)",
            Block(10, Seq(Text(10, "\n  omg"))),
            Some(Block(22, Seq(Text(22, "\n  wtf"))))
          )
        )
        'ifBlockElseBraceBlock - check(
          """@if(true){
            |  omg
            |}else{
            |  wtf
            |}""".stripMargin,
          _.IfElse.run(),
          IfElse(
            0,
            "if(true)",
            Block(10, Seq(Text(10, "\n  omg\n"))),
            Some(Block(23, Seq(Text(23, "\n  wtf\n"))))
          )
        )
        'ifBlockElseBraceBlockNested - {
          val res = Parser(Trim.old(
            """
        @p
          @if(true){
            Hello
          }else{
            lols
          }
            """))
          val expected =
            Block(0, Vector(
              Text(0, "\n"),
              Chain(1, "p",Vector(Block(3, Vector(
                Text(3, "\n  "),
                IfElse(6, "if(true)",
                  Block(16, Vector(
                    Text(16, "\n    Hello\n  ")
                  )),
                  Some(Block(35, Vector(
                    Text(35, "\n    lols\n  ")
                  )))
                ))))),
              Text(48, "\n")
            ))
          assert(res == expected)
        }
        'ifElseBlock - check(
          """@if(true){
            |  omg
            |}else
            |  wtf""".stripMargin,
          _.IfElse.run(),
          IfElse(
            0,
            "if(true)",
            Block(10, Seq(Text(10, "\n  omg\n"))),
            Some(Block(22, Seq(Text(22, "\n  wtf"))))
          )
        )
      }

    }
    'Body{
      'indents - check(
        """
          |@omg
          |  @wtf
          |    @bbq
          |      @lol""".stripMargin,
        _.Body.run(),
        Block(0, Seq(
          Text(0, "\n"),
          Chain(1, "omg",Seq(Block(5, Seq(
            Text(5, "\n  "),
            Chain(8, "wtf",Seq(Block(7, Seq(
              Text(7, "\n    "),
              Chain(12, "bbq",Seq(Block(9, Seq(
                Text(9, "\n      "),
                Chain(16, "lol",Seq())
              ))))
            ))))
          ))))
        ))
      )
      'dedents - check(
        """
          |@omg
          |  @wtf
          |@bbq""".stripMargin,
        _.Body.run(),
        Block(0,
          Seq(
          Text(0, "\n"),
          Chain(1, "omg",Seq(Block(5,
            Seq(
              Text(5, "\n  "),
              Chain(8, "wtf",Seq())
            )
          ))),
          Text(12, "\n"),
          Chain(13, "bbq", Seq())
        ))
      )
      'braces - check(
        """
          |@omg{
          |  @wtf
          |}
          |@bbq""".stripMargin,
        _.Body.run(),
        Block(0, Seq(
          Text(0, "\n"),
          Chain(1, "omg",Seq(Block(6,
            Seq(
              Text(6, "\n  "),
              Chain(9, "wtf",Seq()),
              Text(13, "\n")
            )
          ))),
          Text(15, "\n"),
          Chain(16, "bbq", Seq())
        ))
      )
      'dedentText - check(
        """
          |@omg("lol", 1, 2)
          |  @wtf
          |bbq""".stripMargin,
        _.Body.run(),
        Block(0, Seq(
          Text(0, "\n"),
          Chain(1, "omg",Seq(
            Args(5, """("lol", 1, 2)"""),
            Block(18, Seq(
              Text(18, "\n  "),
              Chain(21, "wtf",Seq())
            ))
          )),
          Text(25, "\nbbq")
        ))
      )
      'weird - check(
        """
          |@omg("lol",
          |1,
          |       2
          |    )
          |  wtf
          |bbq""".stripMargin,
        _.Body.run(),
        Block(0, Seq(
          Text(0, "\n"),
          Chain(1, "omg",Seq(
            Args(5, "(\"lol\",\n1,\n       2\n    )"),
            Block(30, Seq(
              Text(30, "\n  "), Text(33, "wtf")
            ))
          )),
          Text(36, "\n"),
          Text(37, "bbq")
        ))
      )
      'codeBlock - check(
        """@{
          |  val omg = "omg"
          |  omg * 2
          |}""".stripMargin,
        _.Code.run(),
        """{
        |  val omg = "omg"
        |  omg * 2
        |}""".stripMargin
      )
      'codeBlocks - check(
        """
          |@{"lol" * 3}
          |@{
          |  val omg = "omg"
          |  omg * 2
          |}""".stripMargin,
        _.Body.run(),
        Block(0, Seq(
          Text(0, "\n"),
          Chain(1, "{\"lol\" * 3}", Seq()),
          Text(13, "\n"),
          Chain(14, """{
            |  val omg = "omg"
            |  omg * 2
            |}""".stripMargin,
            Seq()
          )
        ))
      )
    }
//    'Test{
//      check(
//        "@{() => ()}",
//        _.Code.run(),
//        ""
//      )
//    }
  }
}



