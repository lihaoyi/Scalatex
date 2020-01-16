package scalatex



import scalatex.stages.{Trim, Parser, Ast}
import Ast.Chain.Args
import Ast._
import utest._
import fastparse._

object ParserTests extends utest.TestSuite{

  def check[T](input: String, parser: P[_] => P[T], expected: T) = {
    parse(input, parser) match{
      case s: fastparse.Parsed.Success[T] =>
        val result = s.value
        assert(result == expected)
      case f: fastparse.Parsed.Failure => throw new Exception(f.extra.trace(true).longAggregateMsg)
    }
  }

  def tests = Tests {
    val p = new Parser()
    import p._

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
      'dontDropTrailingWhitespace - {

        val trimmed = wrap(stages.Trim.old(
          Seq(
            "  i am a cow   ",
            "    hear me moo    ",
            "   i weigh twice as much as you"
          ).mkString("\n")
        ))
        val expected = wrap(
          Seq(
            "i am a cow   ",
            "  hear me moo    ",
            " i weigh twice as much as you"
          ).mkString("\n")
        )
        assert(trimmed == expected)
      }
    }
    'Text {
      println("Checking...")
      * - check("i am a cow", Text(_), Block.Text(0, "i am a cow"))
      * - check("i am a @cow", Text(_), Block.Text(0, "i am a "))
      * - check("i am a @@cow", Text(_), Block.Text(0, "i am a "))
      * - check("i am a @@@cow", Text(_), Block.Text(0, "i am a "))
      * - check("i am a @@@@cow", Text(_), Block.Text(0, "i am a "))
    }
    'Code{
      'identifier - check("gggg  ", Code(_), "gggg")
      'parens - check("(1 + 1)lolsss\n", Code(_),  "(1 + 1)")
      'curlies - check("{{1} + (1)}  ", Code(_), "{{1} + (1)}")
      'blocks - check("{val x = 1; 1} ", Code(_), "{val x = 1; 1}")
      'weirdBackticks - check("{`{}}{()@`}\n", Code(_), "{`{}}{()@`}")
     }
    'MiscCode{
      'imports{
        * - check("import math.abs", Header(_), "import math.abs")
        * - check("import math.{abs, sin}", Header(_), "import math.{abs, sin}")
      }
      'headerblocks{
        check(
          """import math.abs
            |@import math.sin
            |
            |hello world
            |""".stripMargin,
          HeaderBlock(_),
          Ast.Header(
            0,
            "import math.abs\nimport math.sin",
            Block(32,
              Seq(Block.Text(32, "\n\nhello world\n"))
            )
          )
        )
      }
      'caseclass{
        check(
          """case class Foo(i: Int, s: String)""".stripMargin,
          Header(_),
          "case class Foo(i: Int, s: String)"
        )
      }

    }
    'Block{
      * - check("{i am a cow}", BraceBlock(_), Block(1, Seq(Block.Text(1, "i am a cow"))))
      * - check("{i @am a @cow}", BraceBlock(_),
        Block(1, Seq(
          Block.Text(1, "i "),
          Chain(4, "am",Seq()),
          Block.Text(6, " a "),
          Chain(10, "cow",Seq())
        ))
      )
    }
    'Chain{
      * - check("omg.bbq[omg].fff[fff](123)  ", ScalaChain(_),
        Chain(0, "omg",Seq(
          Chain.Prop(3, "bbq"),
          Chain.TypeArgs(7, "[omg]"),
          Chain.Prop(12, "fff"),
          Chain.TypeArgs(16, "[fff]"),
          Chain.Args(21, "(123)")
        ))
      )
      * - check("omg{bbq}.cow(moo){a @b}\n", ScalaChain(_),
        Chain(0, "omg",Seq(
          Block(4, Seq(Block.Text(4, "bbq"))),
          Chain.Prop(8, "cow"),
          Chain.Args(12, "(moo)"),
          Block(18, Seq(Block.Text(18, "a "), Chain(21, "b", Nil)))
        ))
      )
    }
    'Escaping{
      check(
        """
          |haoyi@@gmail.com""".stripMargin,
        File(_),
        Block(0, Seq(
          Block.Text(0, "\nhaoyi@gmail.com")
        ))
      )
    }
    'ControlFlow{
      'for {
        'for - check(
          "for(x <- 0 until 3){lol}",
          ForLoop(_),
          Block.For(0, "for(x <- 0 until 3)", Block(20, Seq(Block.Text(20, "lol"))))
        )
        'forBlock - check(
          """
            |@for(x <- 0 until 3)
            |  lol""".stripMargin,
          File(_),
          Block(0, Seq(
            Block.Text(0, "\n"),
            Block.For(
              2,
              "for(x <- 0 until 3)",
              Block(21, Seq(Block.Text(21, "\n  lol")))
            )
          ))
        )
        'forBlockBraces - check(
          """
            |@for(x <- 0 until 3){
            |  lol
            |}""".stripMargin,
          File(_),
          Block(0, Seq(
            Block.Text(0, "\n"),
            Block.For(
              2,
              "for(x <- 0 until 3)",
              Block(22, Seq(Block.Text(22, "\n  lol\n")))
            )
          ))
        )
      }
      'ifElse {
        'if - check(
          "if(true){lol}",
          IfElse(_),
          Block.IfElse(0, "if(true)", Block(9, Seq(Block.Text(9, "lol"))), None)
        )
        'ifElse - check(
          "if(true){lol}else{ omg }",
          IfElse(_),
          Block.IfElse(0, "if(true)", Block(9, Seq(Block.Text(9, "lol"))), Some(Block(18, Seq(Block.Text(18, " omg ")))))
        )
        'ifBlock - check(
          """if(true)
            |  omg""".stripMargin,
          IndentIfElse(_),
          Block.IfElse(0, "if(true)", Block(8, Seq(Block.Text(8, "\n  omg"))), None)
        )

        'ifBlockElseBlock - check(
          """if(true)
            |  omg
            |@else
            |  wtf""".stripMargin,
          TTTT(_),
          Block.IfElse(
            0,
            "if(true)",
            Block(8, Seq(Block.Text(8, "\n  omg"))),
            Some(Block(20, Seq(Block.Text(20, "\n  wtf"))))
          )
        )
        'ifBlockElseBraceBlock - check(
          """if(true){
            |  omg
            |}else{
            |  wtf
            |}""".stripMargin,
          IfElse(_),
          Block.IfElse(
            0,
            "if(true)",
            Block(9, Seq(Block.Text(9, "\n  omg\n"))),
            Some(Block(22, Seq(Block.Text(22, "\n  wtf\n"))))
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
            """)).get.value
          val expected =
            Block(0, Vector(
              Block.Text(0, "\n"),
              Chain(2, "p",Vector(Block(3, Vector(
                Block.Text(3, "\n  "),
                Block.IfElse(7, "if(true)",
                  Block(16, Vector(
                    Block.Text(16, "\n    Hello\n  ")
                  )),
                  Some(Block(35, Vector(
                    Block.Text(35, "\n    lols\n  ")
                  )))
                ))))),
              Block.Text(48, "\n")
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
        File(_),
        Block(0, Seq(
          Block.Text(0, "\n"),
          Chain(2, "omg",Seq(Block(5, Seq(
            Block.Text(5, "\n  "),
            Chain(9, "wtf",Seq(Block(12, Seq(
              Block.Text(12, "\n    "),
              Chain(18, "bbq",Seq(Block(21, Seq(
                Block.Text(21, "\n      "),
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
        File(_),
        Block(0,
          Seq(
          Block.Text(0, "\n"),
          Chain(2, "omg",Seq(Block(5,
            Seq(
              Block.Text(5, "\n  "),
              Chain(9, "wtf",Seq())
            )
          ))),
          Block.Text(12, "\n"),
          Chain(14, "bbq", Seq())
        ))
      )
      'braces - check(
        """
          |@omg{
          |  @wtf
          |}
          |@bbq""".stripMargin,
        File(_),
        Block(0, Seq(
          Block.Text(0, "\n"),
          Chain(2, "omg",Seq(Block(6,
            Seq(
              Block.Text(6, "\n  "),
              Chain(10, "wtf",Seq()),
              Block.Text(13, "\n")
            )
          ))),
          Block.Text(15, "\n"),
          Chain(17, "bbq", Seq())
        ))
      )
      'dedentText - check(
        """
          |@omg("lol", 1, 2)
          |  @wtf
          |bbq""".stripMargin,
        File(_),
        Block(0, Seq(
          Block.Text(0, "\n"),
          Chain(2, "omg",Seq(
            Args(5, """("lol", 1, 2)"""),
            Block(18, Seq(
              Block.Text(18, "\n  "),
              Chain(22, "wtf",Seq())
            ))
          )),
          Block.Text(25, "\nbbq")
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
//        _.File,
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
        Code(_),
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
        File(_),
        Block(0, Seq(
          Block.Text(0, "\n"),
          Chain(2, "{\"lol\" * 3}", Seq()),
          Block.Text(13, "\n"),
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
        File(_),
        Block(0,List(
          Block.Text(0,"\nlol\nomg\nwtf\nbbq\n"),
          Chain(18,"body",Vector(
            Block(22,List(Block.Text(22,"\n  "),
              Chain(26,"div",Vector(Block(29,List(Block.Text(29, "\n    "),
                Chain(35,"span",Vector(Block(39,List(Block.Text(39, "\n      "),
                  Chain(47,"lol",Vector())))
                ))
              ))
              ))))))))
      )
    }
////    'Test{
////      check(
////        "@{() => ()}",
////        _.Code,
////        ""
////      )
////    }
  }
}



