import utest._

object ServerTest extends TestSuite {
  val stats = new ustats.Stats
  val server = ustats.MetricsServer(stats)
  val http = server.start("localhost", 10000)
  val tests = Tests {
    test("invalid method") {
      requests
        .post("http://localhost:10000/metrics", check = false)
        .statusCode ==> 405
    }
    test("invalid path") {
      requests
        .get("http://localhost:10000/metricz", check = false)
        .statusCode ==> 404
    }
    test("empty metrics") {
      val res = requests.get("http://localhost:10000/metrics")
      res.statusCode ==> 200
      res.contentType ==> Some("text/plain")
      res.text() ==> ""
    }
    test("metrics") {
      val counter = stats.namedCounter("counter")
      counter += 1
      val res = requests.get("http://localhost:10000/metrics")
      res.statusCode ==> 200
      res.contentType ==> Some("text/plain")
      res.text() ==> "# TYPE counter counter\ncounter 1.0\n"
    }
  }
}
