package ustats

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.HttpHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods

/** A convenience HTTP server to export metrics at the standard /metrics
  * endpoint
  * @param stats the statistics instance to export, or None for exporting the
  *              default instance
  */
class MetricsServer private[ustats] (stats: Option[Stats]) {

  val handler = new HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = {
      if (exchange.getRequestMethod() != Methods.GET) {
        exchange.setStatusCode(405)
        exchange.getOutputStream().close()
        return
      }

      if (exchange.getRequestPath() != "/metrics") {
        exchange.setStatusCode(404)
        exchange.getOutputStream().close()
        return
      }

      exchange.setStatusCode(200)
      exchange
        .getResponseHeaders()
        .put(new HttpString("content-type"), "text/plain")
      stats match {
        case Some(s) => s.writeMetricsTo(exchange.getOutputStream())
        case None    => ustats.writeMetricsTo(exchange.getOutputStream())
      }
      exchange.getOutputStream().close()
    }
  }

  def start(
      host: String = "[::]",
      port: Int = 10000,
      verbose: Boolean = false
  ) = {
    if (!verbose) MetricsServer.silenceJboss()
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(new BlockingHandler(handler))
      .setWorkerOption(
        org.xnio.Options.THREAD_DAEMON.asInstanceOf[org.xnio.Option[Any]],
        true
      )
      .build()
    server.start()
    server
  }
}

object MetricsServer {
  def apply(stats: Stats) = new MetricsServer(Some(stats))

  private def silenceJboss(): Unit = {
    // Some jboss classes litter logs from their static initializers. This is a
    // workaround to stop this rather annoying behavior.
    val tmp = System.err
    System.setErr(null)
    org.jboss.threads.Version.getVersionString() // this causes the static initializer to be run
    System.setErr(tmp)

    // Other loggers print way too much information. Set them to only print
    // interesting stuff.
    val level = java.util.logging.Level.WARNING
    java.util.logging.Logger.getLogger("org.jboss").setLevel(level)
    java.util.logging.Logger.getLogger("org.xnio").setLevel(level)
    java.util.logging.Logger.getLogger("io.undertow").setLevel(level)
  }

}
