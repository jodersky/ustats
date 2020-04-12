package ustats

import util._
import utest._

object UtilTest extends TestSuite {
  val tests = Tests{
    test("snakify") {
      snakify("helloWorld") ==> "hello_world"
      snakify("hello_world") ==> "hello_world"
      snakify("HelloWorld") ==> "hello_world"
      snakify("helloWORLD") ==> "hello_world"
      snakify("HELLOworld") ==> "helloworld"
      snakify("") ==> ""
      snakify(" ") ==> " "
      snakify("hello_world") ==> "hello_world"
      snakify("hello_World") ==> "hello_world"
      snakify("hello_FooBar") ==> "hello_foo_bar"
    }
    test("prometheusName") {
      def prometheusName(base: String, labels: (String, Any)*) = {
        labelify(snakify(base), labels)
      }
      prometheusName("base", "method" -> "b", "answer" -> 42) ==> """base{method="b", answer="42"}"""
      prometheusName("base", "method" -> "CUSTOM") ==> """base{method="CUSTOM"}"""
      prometheusName("base", "method" -> "camelCase") ==> """base{method="camelCase"}"""
      prometheusName("base", "camelCase" -> "camelCase") ==> """base{camelCase="camelCase"}"""
    }
  }
}
