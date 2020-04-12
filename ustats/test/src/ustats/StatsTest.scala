package ustats

import utest._

object Test extends TestSuite {

  def withStats(fct: Stats => Unit) = {
    fct(new Stats)
  }

  val tests = Tests {
    test("basic") {
      withStats { s =>
        val myFirstCounter = s.counter("label" -> 1)
        s.metrics ==> "my_first_counter{label=\"1\"} 0.0\n"

        myFirstCounter += 1
        s.metrics ==> "my_first_counter{label=\"1\"} 1.0\n"
        myFirstCounter += 1
        s.metrics ==> "my_first_counter{label=\"1\"} 2.0\n"
      }
    }
    test("many blocks") {
      withStats { s =>
        // this tests that there are no logic errors when traversing metrics split
        // over several blocks
        for (i <- 0 until 100) {
          s.namedCounter(name = s"counter_$i", "a" -> 2)
        }
        val expected =
          (0 until 100).map(i => s"counter_$i" + "{a=\"2\"} 0.0\n").mkString
        s.metrics ==> expected
      }
    }
    test("histogram")(withStats { s =>
      val myHist = s.histogram(Seq(0, 1, 2))
      myHist.add(0.5)
      myHist.add(1.5)
      myHist.add(2)
      myHist.add(10)

      val expected = """|my_hist_bucket{le="0.0"} 0.0
                        |my_hist_bucket{le="1.0"} 1.0
                        |my_hist_bucket{le="2.0"} 3.0
                        |my_hist_bucket{le="+Inf"} 4.0
                        |my_hist_count 4.0
                        |my_hist_sum 14.0
                        |""".stripMargin

      s.metrics ==> expected
    })
  }
}
