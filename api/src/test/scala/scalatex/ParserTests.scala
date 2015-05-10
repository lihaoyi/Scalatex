package scalatex



import scalatex.stages.{Trim, Parser, Ast}
import scalatex.stages.Ast.Block.{IfElse, For, Text}
import Ast.Chain.Args

object ParserTests extends utest.TestSuite{
  import Ast._
  import utest._
  def check[T](input: String, parser: Parser => fastparse.Parser[T], expected: T) = {
    parser(new Parser()).parse(input) match{
      case s: fastparse.Result.Success[T] =>
        val result = s.value
        assert(result == expected)
      case f: fastparse.Result.Failure => throw new Exception(f.trace)
    }
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
      println("Checking...")
      * - check("i am a cow", _.Text, Block.Text(0, "i am a cow"))
      * - check("i am a @cow", _.Text, Block.Text(0, "i am a "))
      * - check("i am a @@cow", _.Text, Block.Text(0, "i am a "))
      * - check("i am a @@@cow", _.Text, Block.Text(0, "i am a "))
      * - check("i am a @@@@cow", _.Text, Block.Text(0, "i am a "))
    }
    'Code{
      'identifier - check("gggg  ", _.Code, "gggg")
      'parens - check("(1 + 1)lolsss\n", _.Code,  "(1 + 1)")
      'curlies - check("{{1} + (1)}  ", _.Code, "{{1} + (1)}")
      'blocks - check("{val x = 1; 1} ", _.Code, "{val x = 1; 1}")
      'weirdBackticks - check("{`{}}{()@`}\n", _.Code, "{`{}}{()@`}")
     }
    'MiscCode{
      'imports{
        * - check("import math.abs", _.Header, "import math.abs")
        * - check("import math.{abs, sin}", _.Header, "import math.{abs, sin}")
      }
      'headerblocks{
        check(
          """import math.abs
            |@import math.sin
            |
            |hello world
            |""".stripMargin,
          _.HeaderBlock,
          Ast.Header(
            0,
            "import math.abs\nimport math.sin",
            Ast.Block(32,
              Seq(Text(32, "\n\nhello world\n"))
            )
          )
        )
      }
      'caseclass{
        check(
          """case class Foo(i: Int, s: String)
          """.stripMargin,
          _.Header,
          "case class Foo(i: Int, s: String)"
        )
      }

    }
    'Block{
      * - check("{i am a cow}", _.BraceBlock, Block(1, Seq(Block.Text(1, "i am a cow"))))
      * - check("{i @am a @cow}", _.BraceBlock,
        Block(1, Seq(
          Block.Text(1, "i "),
          Chain(4, "am",Seq()),
          Block.Text(6, " a "),
          Chain(10, "cow",Seq())
        ))
      )
    }
    'Chain{
      * - check("omg.bbq[omg].fff[fff](123)  ", _.ScalaChain,
        Chain(0, "omg",Seq(
          Chain.Prop(3, "bbq"),
          Chain.TypeArgs(7, "[omg]"),
          Chain.Prop(12, "fff"),
          Chain.TypeArgs(16, "[fff]"),
          Chain.Args(21, "(123)")
        ))
      )
      * - check("omg{bbq}.cow(moo){a @b}\n", _.ScalaChain,
        Chain(0, "omg",Seq(
          Block(4, Seq(Text(4, "bbq"))),
          Chain.Prop(8, "cow"),
          Chain.Args(12, "(moo)"),
          Block(18, Seq(Text(18, "a "), Chain(21, "b", Nil)))
        ))
      )
    }
    'ControlFlow{
      'for {
        'for - check(
          "for(x <- 0 until 3){lol}",
          _.ForLoop,
          For(0, "for(x <- 0 until 3)", Block(20, Seq(Text(20, "lol"))))
        )
        'forBlock - check(
          """
            |@for(x <- 0 until 3)
            |  lol""".stripMargin,
          _.Body,
          Block(0, Seq(
            Text(0, "\n"),
            For(
              2,
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
          _.Body,
          Block(0, Seq(
            Text(0, "\n"),
            For(
              2,
              "for(x <- 0 until 3)",
              Block(22, Seq(Text(22, "\n  lol\n")))
            )
          ))
        )
      }
      'ifElse {
        'if - check(
          "if(true){lol}",
          _.IfElse,
          IfElse(0, "if(true)", Block(9, Seq(Text(9, "lol"))), None)
        )
        'ifElse - check(
          "if(true){lol}else{ omg }",
          _.IfElse,
          IfElse(0, "if(true)", Block(9, Seq(Text(9, "lol"))), Some(Block(18, Seq(Text(18, " omg ")))))
        )
        'ifBlock - check(
          """
            |@if(true)
            |  omg""".stripMargin,
          _.IndentIfElse,
          IfElse(2, "if(true)", Block(10, Seq(Text(10, "\n  omg"))), None)
        )
        'ifBlockElseBlock - check(
          """
            |@if(true)
            |  omg
            |@else
            |  wtf""".stripMargin,
          _.IndentIfElse,
          IfElse(
            2,
            "if(true)",
            Block(10, Seq(Text(10, "\n  omg"))),
            Some(Block(22, Seq(Text(22, "\n  wtf"))))
          )
        )
        'ifBlockElseBraceBlock - check(
          """if(true){
            |  omg
            |}else{
            |  wtf
            |}""".stripMargin,
          _.IfElse,
          IfElse(
            0,
            "if(true)",
            Block(9, Seq(Text(9, "\n  omg\n"))),
            Some(Block(22, Seq(Text(22, "\n  wtf\n"))))
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
              Chain(2, "p",Vector(Block(3, Vector(
                Text(3, "\n  "),
                IfElse(7, "if(true)",
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
      }

    }
    'Body{
      'indents - check(
        """
          |@omg
          |  @wtf
          |    @bbq
          |      @lol""".stripMargin,
        _.Body,
        Block(0, Seq(
          Text(0, "\n"),
          Chain(2, "omg",Seq(Block(5, Seq(
            Text(5, "\n  "),
            Chain(9, "wtf",Seq(Block(12, Seq(
              Text(12, "\n    "),
              Chain(18, "bbq",Seq(Block(21, Seq(
                Text(21, "\n      "),
                Chain(29, "lol",Seq())
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
        _.Body,
        Block(0,
          Seq(
          Text(0, "\n"),
          Chain(2, "omg",Seq(Block(5,
            Seq(
              Text(5, "\n  "),
              Chain(9, "wtf",Seq())
            )
          ))),
          Text(12, "\n"),
          Chain(14, "bbq", Seq())
        ))
      )
      'braces - check(
        """
          |@omg{
          |  @wtf
          |}
          |@bbq""".stripMargin,
        _.Body,
        Block(0, Seq(
          Text(0, "\n"),
          Chain(2, "omg",Seq(Block(6,
            Seq(
              Text(6, "\n  "),
              Chain(10, "wtf",Seq()),
              Text(13, "\n")
            )
          ))),
          Text(15, "\n"),
          Chain(17, "bbq", Seq())
        ))
      )
      'dedentText - check(
        """
          |@omg("lol", 1, 2)
          |  @wtf
          |bbq""".stripMargin,
        _.Body,
        Block(0, Seq(
          Text(0, "\n"),
          Chain(2, "omg",Seq(
            Args(5, """("lol", 1, 2)"""),
            Block(18, Seq(
              Text(18, "\n  "),
              Chain(22, "wtf",Seq())
            ))
          )),
          Text(25, "\nbbq")
        ))
      )
//      'weird - check(
//        """
//          |@omg("lol",
//          |1,
//          |       2
//          |    )
//          |  wtf
//          |bbq""".stripMargin,
//        _.Body,
//        Block(0, Seq(
//          Text(0, "\n"),
//          Chain(1, "omg",Seq(
//            Args(5, "(\"lol\",\n1,\n       2\n    )"),
//            Block(30, Seq(
//              Text(30, "\n  "), Text(33, "wtf")
//            ))
//          )),
//          Text(36, "\n"),
//          Text(37, "bbq")
//        ))
//      )
      'codeBlock - check(
        """{
          |  val omg = "omg"
          |  omg * 2
          |}""".stripMargin,
        _.Code,
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
        _.Body,
        Block(0, Seq(
          Text(0, "\n"),
          Chain(2, "{\"lol\" * 3}", Seq()),
          Text(13, "\n"),
          Chain(15, """{
            |  val omg = "omg"
            |  omg * 2
            |}""".stripMargin,
            Seq()
          )
        ))
      )
      'nesting - check(
        """
          |lol
          |omg
          |wtf
          |bbq
          |@body
          |  @div
          |    @span
          |      @lol""".stripMargin,
        _.Body,
        Block(0,List(
          Text(0,"\nlol\nomg\nwtf\nbbq\n"),
          Chain(18,"body",Vector(
            Block(22,List(Text(22,"\n  "),
              Chain(26,"div",Vector(Block(29,List(Text(29, "\n    "),
                Chain(35,"span",Vector(Block(39,List(Text(39, "\n      "),
                  Chain(47,"lol",Vector())))
                ))
              ))
              ))))))))
      )
    }
//    'Test{
//      check(
//        "@{() => ()}",
//        _.Code,
//        ""
//      )
//    }
  }
}



