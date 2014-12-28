package scalatex.site

import java.nio.file.{Paths, Files}

import utest._
import scala.collection.mutable
import scalatags.Text.all
import scalatags.Text.all._

import scalatex.site.Section.Tree

object Tests extends TestSuite{
  def cmp(s1: String, s2: String) = {
    val f1 = s1.filter(!_.isWhitespace).mkString
    val f2 = s2.filter(!_.isWhitespace)
    assert(f1 == f2)
  }
  val tests = TestSuite{
    'Hello{
      cmp(
        Hello().render
        ,
        """
        <div>
          Hello World

          <h1>I am a cow!</h1>
        </div>
        """
      )
    }

    'Section{
      object sect extends Section
      val txt = sect("Main")(
        sect("SectionA")(
          "Hello World"
        ),
        sect("SectionB0")(
          sect("SectionB1")(
            "Lols", sect.ref("Main")
          ),
          sect("SectionB2")(
            "Hai", sect.ref("SectionA")
          )
        )
      ).render
      // 1s are raw-text, 3s are titles, and 5s are
      // titles which are linked to somewhere
      val expectedTokens = Seq(
        "Main" -> 5,
        "SectionA" -> 5,
        "Hello World" -> 1,
        "SectionB0" -> 3,
        "SectionB1" -> 3,
        "SectionB2" -> 3,
        "Lols" -> 1,
        "Hai" -> 1
      )
      for ((token, count) <- expectedTokens){
        assert(token.r.findAllIn(txt).length == count)
      }
      val expectedTree =Tree[String]("root", mutable.Buffer(
        Tree[String]("Main", mutable.Buffer(
          Tree[String]("SectionA", mutable.Buffer()),
          Tree[String]("SectionB0", mutable.Buffer(
            Tree[String]("SectionB1", mutable.Buffer()),
            Tree[String]("SectionB2", mutable.Buffer())
          ))
        ))
      ))
      val struct = sect.structure
      assert(struct == expectedTree)
      assert(sect.usedRefs == Set("Main", "SectionA"))
    }
    'Highlighter{
      object hl extends Highlighter {
        def suffixMappings = Map("scala" -> "scala")
      }
      'wholeFile {
        val (start, end, txt) = hl.referenceText(
          "site/src/test/scala/scalatex/site/Tests.scala",
          Nil,
          Nil
        )
        val expected = io.Source.fromFile("site/src/test/scala/scalatex/site/Tests.scala").mkString
        cmp(txt, expected)
      }
      'selectors {
        /**
         * Comment I'm trawling for
         */
        val (start, end, txt) = hl.referenceText(
          "site/src/test/scala/scalatex/site/Tests.scala",
          Seq("Highlighter", "selectors", "/**"),
          Seq("*/", "val")
        )

        val expected =
          """
            /**
             * Comment I'm trawling for
             */
          """
        cmp(txt, expected)
      }
      'trimBlanks{
        // Make sure that indentaton and trailing blank lines get removed

        // trimBlanks start

        // trimBlanks content

        // trimBlanks end
        val (start, end, txt) = hl.referenceText(
          "site/src/test/scala/scalatex/site/Tests.scala",
          "trimBlanks start",
          "trimBlanks end"
        )
        val expected =
          """// trimBlanks start
            |
            |// trimBlanks content""".stripMargin
        assert(txt == expected)
      }
      'dedentEnd{
        // Make sure any dedentation from the starting line ends
        // the snippet, even if no explicit ending is specified.
        val test = {
          Seq("Hello! ",
            "I am a cow"
          )
        }
        val (start, end, txt) = hl.referenceText(
          "site/src/test/scala/scalatex/site/Tests.scala",
          Seq("dedentEnd", "val test", "Hello!"),
          Nil
        )
        val expected =
          """Seq("Hello! ",
            |  "I am a cow"
            |)""".stripMargin
        assert(txt == expected)
      }
    }
    'Site{
      def delete(file: java.io.File): Array[(String, Boolean)] = {
        Option(file.listFiles)
          .map(_.flatMap(f => delete(f)))
          .getOrElse(Array()) :+ (file.getPath -> file.delete)
      }
      'simple {
        val site = new Site {
          def content = Map("index.html" -> Hello())
        }
        site.renderTo("site/target/output/")

        def check() = {
          val readText = io.Source.fromFile("site/target/output/index.html").mkString
          assert(
            readText.contains("Hello World"),
            readText.contains("I am a cow!"),
            readText.contains("<div>"),
            readText.contains("<h1>"),
            Files.exists(Paths.get("site/target/output/scripts.js")),
            Files.exists(Paths.get("site/target/output/styles.css"))
          )
        }
        check()
        // re-rendering works
        site.renderTo("site/target/output/")

        // deleting and re-rendering works too

        delete(new java.io.File("site/target/output"))
        assert(!new java.io.File("site/target/output").exists())
        site.renderTo("site/target/output/")
        check()
      }
      'overriding{
        delete(new java.io.File("site/target/output2"))
        assert(!new java.io.File("site/target/output2").exists())
        val site = new Site {
          def content = Map(
            "page1.html" -> Hello(),
            "page2.html" -> About()
          )
          override def scriptName = "custom.js"
          override def stylesName = "custom.css"
          override def headFrags = super.headFrags ++ Seq(
            script("console.log('Hi!')")
          )
        }
        site.renderTo("site/target/output2/")
        val page1 = io.Source.fromFile("site/target/output2/page1.html").mkString
        val page2 = io.Source.fromFile("site/target/output2/page2.html").mkString

        assert(
          page1.contains("Hello World"),
          page1.contains("I am a cow!"),
          page1.contains("<div>"),
          page1.contains("<h1>"),
          page2.contains("Hear me moo"),
          Files.exists(Paths.get("site/target/output2/custom.js")),
          Files.exists(Paths.get("site/target/output2/custom.css"))
        )
      }
    }
  }
}

