//package scalatex
//
//import utest._
//
//import scalatags.Text.all._
//
//
//object ExampleTests extends TestSuite{
//  import scalatex.TestUtil._
//  val tests = TestSuite{
//    'helloWorld - check(
//      tw("""
//        @div(id:="my-div")
//          @h1
//            Hello World!
//          @p
//            I am @b{Cow}
//            Hear me moo
//            I weigh twice as much as you
//            And I look good on the barbecue
//      """),
//      """
//        <div id="my-div">
//          <h1>Hello World!</h1>
//          <p>
//            I am <b>Cow</b>
//            Hear me moo
//            I weigh twice as much as you
//            And I look good on the barbecue
//          </p>
//        </div>
//      """
//    )
//    'empty1 - check(
//      tw(
//        """
//        """),
//      ""
//    )
//    'empty2 - check(
//      tw(""""""),
//      ""
//    )
//    'variousSyntaxes - check(
//      tw("""
//          @div
//            @h1("Hello World")
//            @h2{I am Cow}
//            @p
//              Hear me @b{moo}
//              I weigh twice as much as you
//              And I look good on the barbecue
//         """),
//      """
//          <div>
//            <h1>Hello World</h1>
//            <h2>I am Cow</h2>
//            <p>
//              Hear me <b>moo</b>
//              I weigh twice as much as you
//              And I look good on the barbecue
//            </p>
//          </div>
//      """
//    )
//
//    'controlFlow{
//      'loops - check(
//        tw("""
//            @ul
//              @for(x <- 0 until 3)
//                @li
//                  lol @x
//           """),
//        tw("""
//            @ul
//               @for(x <- 0 until 3){@li{lol @x}}
//           """),
//        """
//            <ul>
//              <li>lol 0</li>
//              <li>lol 1</li>
//              <li>lol 2</li>
//            </ul>
//        """
//      )
//
//      'conditionals - check(
//        tw("""
//            @ul
//              @for(x <- 0 until 5)
//                @li
//                  @if(x % 2 == 0)
//                    @b{lol} @x
//                  @else
//                    @i{lol} @x
//           """),
//        """
//            <ul>
//              <li><b>lol</b> 0</li>
//              <li><i>lol</i> 1</li>
//              <li><b>lol</b> 2</li>
//              <li><i>lol</i> 3</li>
//              <li><b>lol</b> 4</li>
//            </ul>
//        """
//      )
//
//      'functions - check(
//        tw("""
//          @span
//            The square root of 9.0
//            is @math.sqrt(9.0)
//        """),
//        """
//          <span>
//            The square root of 9.0 is 3.0
//          </span>
//        """
//      )
//
//    }
//    'interpolation{
//      'simple - check(
//        tw("""
//          @span
//            1 + 2 is @(1 + 2)
//        """),
//        tw("""
//          @span
//            1 + 2 is @{1 + 2}
//        """),
//        """
//          <span>1 + 2 is 3</span>
//        """
//      )
//      'multiline - check(
//        tw("""
//          @div
//            1 + 2 is @{
//              val x = "1"
//              val y = "2"
//              println(s"Adding $x and $y")
//              x.toInt + y.toInt
//            }
//        """),
//
//        """
//          <div>
//            1 + 2 is 3
//          </div>
//        """
//      )
//    }
//    'definitions{
//      'imports - check(
//        tw("""
//          @import scala.math._
//          @ul
//            @for(i <- -2 to 2)
//              @li
//                @abs(i)
//        """),
//        """
//          <ul>
//            <li>2</li>
//            <li>1</li>
//            <li>0</li>
//            <li>1</li>
//            <li>2</li>
//          </ul>
//        """
//      )
//
//      'externalDefinitions{
//        object Stuff {
//          object omg {
//            def wrap(s: Frag*): Frag = Seq[Frag]("...", s, "...")
//          }
//          def str = "hear me moo"
//        }
//        'externalDefinitions2
//        check(
//          tw("""
//          @import Stuff._
//          @omg.wrap
//            i @b{am} cow @str
//             """),
//          """
//            ...i <b>am</b> cow hear me moo...
//          """
//        )
//      }
//      'internalDefinitions{
//
//        check(
//          tw("""
//            @object Foo{
//              val const1 = 1337
//            }
//            @val const2 = 30000 + 1337
//
//            @p
//              @def wrap(s: Frag*) = span(u(s))
//
//              The first constant is @Foo.const1,
//              the second is @const2, and wrapping
//              looks like @wrap{hello @b{world}}
//              or @wrap("hello ", b("world"))
//          """),
//          """
//            <p>
//              The first constant is 1337, the second
//              is 31337, and wrapping looks like
//              <span><u>hello <b>world</b></u></span> or
//              <span><u>hello <b>world</b></u></span>
//            </p>
//          """
//        )
//      }
//      'parensNewLine{
//        check(
//          tw("""
//            @div(
//              "hello"
//            )
//          """),
//          """<div>hello</div>"""
//        )
//      }
//      'thingy {
//        check(
//          tw("""
//            @val x = {1}
//            @val y = {2}
//
//            @x + @y is @(x + y)
//          """),
//          """
//            1 + 2 is 3
//          """
//        )
//      }
//    }
//
//  }
//}
