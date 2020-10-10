package ustats

import utest._

object Test extends TestSuite {

  def withStats(fct: Stats => Unit) = {
    fct(Stats())
  }

  val tests = Tests {
    test("basic") {
      withStats { s =>
        val myFirstCounter = s.counter("label" -> 1)
        s.metrics(false) ==> "my_first_counter{label=\"1\"} 0.0\n"

        myFirstCounter += 1
        s.metrics(false) ==> "my_first_counter{label=\"1\"} 1.0\n"
        myFirstCounter += 1
        s.metrics(false) ==> "my_first_counter{label=\"1\"} 2.0\n"
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
        s.metrics(false) ==> expected
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

      s.metrics(false) ==> expected
    })
    test("types") {
      test("counter") {
        withStats { s =>
          val myFirstCounter = s.counter("label" -> 1)
          s.metrics() ==> """|# TYPE my_first_counter counter
                             |my_first_counter{label="1"} 0.0
                             |""".stripMargin
        }
      }
      test("gauge") {
        withStats { s =>
          val megaGauge = s.gauge("label" -> 1)
          s.metrics() ==> """|# TYPE mega_gauge gauge
                             |mega_gauge{label="1"} 0.0
                             |""".stripMargin
        }
      }
      test("histogram") {
        withStats { s =>
          val myHist = s.histogram(Seq(0, 1, 2))
          s.metrics() ==> """|# TYPE my_hist histogram
                             |my_hist_bucket{le="0.0"} 0.0
                             |my_hist_bucket{le="1.0"} 0.0
                             |my_hist_bucket{le="2.0"} 0.0
                             |my_hist_bucket{le="+Inf"} 0.0
                             |my_hist_count 0.0
                             |my_hist_sum 0.0
                             |""".stripMargin
        }
      }
    }
    test("help") {
      withStats { s =>
        val myFirstCounter =
          s.counter(help = "some random counter", "label" -> 1)
        s.metrics() ==> """|# HELP my_first_counter some random counter
                           |# TYPE my_first_counter counter
                           |my_first_counter{label="1"} 0.0
                           |""".stripMargin
      }
    }
  }
}
