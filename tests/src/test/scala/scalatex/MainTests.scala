package scalatex
import utest._
object MainTests extends TestSuite{
  val tests = TestSuite{
    "Hello World" - assert(Hello().render == "Hello world!")

  }
}
