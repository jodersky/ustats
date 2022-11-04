object Main extends cask.MainRoutes {

  val httpRequestsSeconds = ustats.global.histogram("http_requests_seconds", labels = Seq("path" -> "/index"))
  val randomFailures = ustats.global.counter("random_failures")

  @cask.get("/")
  def index() = httpRequestsSeconds.time {
    if (util.Random.nextBoolean()) {
      randomFailures += 1
    }
    cask.Response("here you are!", 200)
  }

  @cask.get("/metrics")
  def metrics() = ustats.global.metrics()

  initialize()

}
