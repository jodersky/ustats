package ustats

/** Default global stats collector. */
object global extends Metrics

object test:
  @main
  def foo =
    global.counter("test").inc()
