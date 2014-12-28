package scalatex
import utest._
import scala.collection.mutable.ArrayBuffer
import scalatex.stages._
import scalatags.Text.all._


/**
* Created by haoyi on 7/14/14.
*/
object BasicTests extends TestSuite{
  import TestUtil._

  val tests = TestSuite{


    'interpolation{
      'chained-check(
        tw("omg @scala.math.pow(0.5, 3) wtf"),
        "omg 0.125 wtf"
      )
      'parens-check(
        tw("omg @(1 + 2 + 3 + 4) wtf"),
        "omg 10 wtf"
      )
      'block-check(
        tw("""
          @{"lol" * 3}
          @{
            val omg = "omg"
            omg * 2
          }
           """),
        """
          lollollol
          omgomg
        """
      )
    }
    'definitions{
      'imports{
        object Whee{
          def func(x: Int) = x * 2
        }
        check(
          tw("""
            @import math._
            @import Whee.func
            @abs(-10)
            @p
              @max(1, 2)
              @func(2)
          """),
          """
            10
            <p>
              2
              4
            </p>
          """
        )
      }
      'valDefVar{
        check(
          tw("""
            Hello
            @val x = 1
            World @x
            @def y = "omg"
            mooo
            @y
          """),
          """
            Hello
            World 1
            mooo
            omg
          """
        )
      }
      'classObjectTrait{
        check(
          tw("""
            @trait Trait{
              def tt = 2
            }
            Hello
            @case object moo extends Trait{
              val omg = "wtf"
            }

            @moo.toString
            @moo.omg
            @case class Foo(i: Int, s: String, b: Boolean)
            TT is @moo.tt
            @Foo(10, "10", true).toString
          """),
          """
            Hello
            moo
            wtf
            TT is 2
            Foo(10, 10, true)
          """
        )
      }
    }
    'parenArgumentLists{
      'attributes{
        check(
          tw("""
            @div(id:="my-id"){ omg }
            @div(id:="my-id")
              omg
          """),
          """
            <divid="my-id">omg</div>
            <divid="my-id">omg</div>
          """
        )
      }
//      'multiline{
//
//        check(
//          tw("""
//          @div(
//            h1("Hello World"),
//            p("I am a ", b{"cow"})
//          )
//          """),
//          """
//          <div>
//            <h1>Hello World</h1>
//            <p>I am a <b>cow</b></p>
//          </div>
//          """
//        )
//      }
    }
    'grouping{
      'negative{
        // The indentation for "normal" text is ignored; we only
        // create blocks from the indentation following a scala
        // @xxx expression
        check(
          tw("""
          I am cow hear me moo
              I weigh twice as much as you
                And I look good on the barbecue
          Yoghurt curds cream cheese and butter
            Comes from liquids from my udder
              I am cow I am cow hear me moooooo
          """),
          """
          I am cow hear me moo
            I weigh twice as much as you
              And I look good on the barbecue
                Yoghurt curds cream cheese and butter
                  Comes from liquids from my udder
                    I am cow I am cow hear me moooooo
          """
        )
      }
      'indentation{
        'simple{
          val world = "World2"

          check(
            tw("""
              @h1
                Hello World
              @h2
                hello @world
              @h3
                Cow
            """),
            """
              <h1>HelloWorld</h1>
              <h2>helloWorld2</h2>
              <h3>Cow</h3>
            """
          )
        }
        'linearNested{
          check(
            tw("""
              @h1 @span @a Hello World
              @h2 @span @a hello
                  @b world
              @h3 @i
                  @div Cow
            """),
            """
              <h1></h1><span></span><a></a>HelloWorld
              <h2></h2><span></span><a></a>hello<b></b>world
              <h3></h3><i></i><div></div>Cow
            """
          )
        }
        'crasher{
          tw("""
@html
    @head
        @meta
    @div
        @a
            @span
          """)
        }
      }
      'curlies{
        'simple{
          val world = "World2"

          check(
            tw("""@div{Hello World}"""),
            """<div>HelloWorld</div>"""
          )
        }
        'multiline{
          check(
            tw("""
              @div{
                Hello
              }
            """),
            """
              <div>Hello</div>
            """
          )
        }
      }
      'mixed{
        check(
          tw("""
            @div{
              Hello
              @div
                @h1
                  WORLD @b{!!!}
                  lol
                @p{
                  @h2{Header 2}
                }
            }
          """),
          """
            <div>
              Hello
              <div>
                <h1>WORLD<b>!!!</b>lol</h1>
                <p><h2>Header2</h2></p>
              </div>
            </div>
          """
        )
      }
//
//      'args{
//        val things = Seq(1, 2, 3)
//        check(
//          tw("""
//            @ul
//              @things.map { x =>
//                @li
//                  @x
//              }
//          """),
//          tw("""
//            @ul
//              @things.map x =>
//                @li
//                  @x
//
//          """),
//          """
//            <ul>
//              <li>1</li>
//              <li>2</li>
//              <li>3</li>
//            </ul>
//          """
//        )
//      }
    }
//
    'loops {
//
      * - check(
        tw("""
          @for(x <- 0 until 3)
            lol
        """),
        tw("""
          @for(x <- 0 until 3){
            lol
          }
        """),
        "lollollol"
      )


      * - check(
          tw("""
            @p
              @for(x <- 0 until 2)
                @for(y <- 0 until 2)
                  lol@x@y
          """),
          tw( """
            @p
              @for(x <- 0 until 2){
                @for(y <- 0 until 2)
                  lol@x@y
              }
          """),
          tw("""
            @p
              @for(x <- 0 until 2)
                @for(y <- 0 until 2){
                  lol@x@y
                }
          """),
          "<p>lol00lol01lol10lol11</p>"
        )
        check(
          tw("""
            @p
              @for(x <- 0 until 2)
                @for(y <- 0 until 2)
                  lol@x@y
          """),
          "<p>lol00lol01lol10lol11</p>"
        )

      * - check(
          tw(
            """
            @for(x <- 0 until 2; y <- 0 until 2)
              @div{@x@y}

          """),
        """<div>00</div><div>01</div><div>10</div><div>11</div>"""
        )
    }

    'ifElse{
      'basicExamples{
        * - check(
          tw("""
            @if(false)
              Hello
            @else
              lols
            @p
          """),
          "lols<p></p>"
        )


        * - check(
          tw("""
            @div
              @if(true)
                Hello
              @else
                lols
          """),
          "<div>Hello</div>"
        )

        * - check(
          tw("""@div
              @if(true)
                Hello
              @else
                lols
          """),
          "<div>Hello</div>"
        )
        * - check(
          tw("""
            @if(false)
              Hello
            @else
              lols
          """),
          "lols"
        )
        * - check(
          tw("""
            @if(false)
              Hello
            @else
              lols
            @img
          """),
          "lols<img/>"
        )
        * - check(
          tw("""
            @p
              @if(true)
                Hello
              @else
                lols
          """),
          tw("""
            @p
              @if(true){
                Hello
              }else{
                lols
              }
          """),
          "<p>Hello</p>"
        )
      }
//      'funkyExpressions{
//        * - check(
//          tw("""
//            @p
//              @if(true == false == (true.==(false)))
//                @if(true == false == (true.==(false)))
//                  Hello1
//                @else
//                  lols1
//              @else
//                @if(true == false == (true.==(false)))
//                  Hello2
//                @else
//                  lols2
//          """),
//          "<p>Hello1</p>"
//        )
//        * - check(
//          tw("""
//            @p
//              @if(true == false != (true.==(false)))
//                @if(true == false != (true.==(false)))
//                  Hello1
//                @else
//                  lols1
//              @else
//                @if(true == false != (true.==(false)))
//                  Hello2
//                @else
//                  lols2
//          """),
//          "<p>lols2</p>"
//        )
//      }
    }

    'files{
      * - check(
        twf("api/src/test/resources/scalatex/success/Simple.scalatex"),
        "Hello world!"
      )
      * - check(
        twf("api/src/test/resources/scalatex/success/Nested.scalatex"),
        """
          <p>Hello Everyone!</p>
          <div>
            <h1>I am a cow</h1>
            <p>
              And so are you: 1
              <img></img>
            </p>
            <p>
              And so are you: 2
              <img></img>
            </p>
            <p>
              And so are you: 3
              <img></img>
            </p>
          </div>"""
      )
    }
  }

}
