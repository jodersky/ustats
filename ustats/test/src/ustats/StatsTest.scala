package ustats

import utest._

object Test extends TestSuite {

  def withStats(fct: Stats => Unit) = {
    fct(new Stats())
  }

  val tests = Tests {
    test("basic") {
      withStats { s =>
        val myFirstCounter =
          s.counter(
            "my_first_counter",
            labels = Seq("label1" -> 1, "label2" -> 2)
          )
        s.metrics(false) ==> "my_first_counter{label1=\"1\", label2=\"2\"} 0.0\n"

        myFirstCounter += 1
        s.metrics(false) ==> "my_first_counter{label1=\"1\", label2=\"2\"} 1.0\n"
        myFirstCounter += 1
        s.metrics(false) ==> "my_first_counter{label1=\"1\", label2=\"2\"} 2.0\n"
      }
    }
    test("many blocks") {
      withStats { s =>
        // this tests that there are no logic errors when traversing metrics split
        // over several blocks
        for (i <- 0 until 1000) {
          s.counter(name = s"counter_$i", labels = Seq("a" -> 2))
        }
        val expected =
          (0 until 1000).map(i => s"counter_$i" + "{a=\"2\"} 0.0\n").mkString
        s.metrics(false) ==> expected
      }
    }
    test("histogram")(withStats { s =>
      val myHist = s.histogram("my_hist", buckets = Seq(0, 1, 2))
      myHist.observe(0.5)
      myHist.observe(1.5)
      myHist.observe(2)
      myHist.observe(10)

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
          val myFirstCounter = s.counter("my_first_counter")
          s.metrics() ==> """|# TYPE my_first_counter counter
                             |my_first_counter 0.0
                             |""".stripMargin
        }
      }
      test("gauge") {
        withStats { s =>
          val megaGauge = s.gauge("mega_gauge")
          s.metrics() ==> """|# TYPE mega_gauge gauge
                             |mega_gauge 0.0
                             |""".stripMargin
        }
      }
      test("histogram") {
        withStats { s =>
          val myHist = s.histogram("my_hist", buckets = Seq(0, 1, 2))
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
          s.counter("my_first_counter", help = "some random counter")
        s.metrics() ==> """|# HELP my_first_counter some random counter
                           |# TYPE my_first_counter counter
                           |my_first_counter 0.0
                           |""".stripMargin
      }
    }
    test("labels") {
      withStats { s =>
        val c = s.counters("some_counter", labels = Seq("l1", "l2"))
        s.metrics(false) ==> ""

        intercept[IllegalArgumentException] {
          c.labelled(1) // too few
        }
        intercept[IllegalArgumentException] {
          c.labelled(1, 2, 3) // too many
        }

        c.labelled(1, 2)
        s.metrics(false) ==> "some_counter{l1=\"1\", l2=\"2\"} 0.0\n"
        c.labelled(1, 2) += 1
        s.metrics(false) ==> "some_counter{l1=\"1\", l2=\"2\"} 1.0\n"

        c.labelled(2, 2) += 1
        s.metrics(false) ==> "some_counter{l1=\"1\", l2=\"2\"} 1.0\nsome_counter{l1=\"2\", l2=\"2\"} 1.0\n"

      }
    }
  }
}
