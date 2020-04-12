object Main extends cask.MainRoutes {

  val httpRequestsSeconds = ustats.histogram("path" -> "/index")
  val randomFailures = ustats.counter()

  @cask.get("/")
  def index() = httpRequestsSeconds.time {
    if (util.Random.nextBoolean()) {
      randomFailures += 1
    }
    cask.Response("here you are!", 200)
  }

  @cask.get("/metrics")
  def metrics() = ustats.metrics

  initialize()

}
