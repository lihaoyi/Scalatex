package scalatex

import utest._

import scalatags.Text.all

/**
 * Created by haoyi on 12/23/14.
 */
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

  }
}
object sect extends site.Section