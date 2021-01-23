import utest._

object ServerTest extends TestSuite {

  case class Response(statusCode: Int, body: String)

  def get(url: String): Response = {
    val url1 = new java.net.URL(url)
    val conn = url1.openConnection().asInstanceOf[java.net.HttpURLConnection]
    try {
      conn.setRequestMethod("GET")
      val code = conn.getResponseCode()

      if (200 <= code && code < 300) {
        val data = new java.io.ByteArrayOutputStream()
        val stream = conn.getInputStream()

        try {
          val tmp = new Array[Byte](8192)

          var c = 0
          while ({c = stream.read(tmp); c > 0}) {
            data.write(tmp, 0, c)
          }
        } finally stream.close()

        val message = new String(data.toByteArray(), "utf-8")
        Response(code, message)
      } else {
        Response(code, "")
      }
    } finally conn.disconnect()

  }


  val stats = new ustats.Stats
  val server = ustats.MetricsServer(stats)
  val http = server.start("localhost", 10000)
  val tests = Tests {
    test("invalid path") {
      get("http://localhost:10000/metricz")
        .statusCode ==> 404
    }
    test("empty metrics") {
      val res = get("http://localhost:10000/metrics")
      res.statusCode ==> 200
      res.body ==> ""
    }
    test("metrics") {
      val counter = stats.counter("counter")
      counter += 1
      val res = get("http://localhost:10000/metrics")
      res.statusCode ==> 200
      res.body ==> "# TYPE counter counter\ncounter 1.0\n"
    }
  }
}
