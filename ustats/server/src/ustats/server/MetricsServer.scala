package ustats.server

import io.undertow.Undertow
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.HttpHandler
import io.undertow.util.HttpString
import io.undertow.util.Methods

class MetricsServer(metrics: ustats.Metrics*):
  val handler = new HttpHandler:
    override def handleRequest(req: HttpServerExchange): Unit =
      if req.getRequestMethod() != Methods.GET then
        req.setStatusCode(405)
        req.getOutputStream().close()
        return
      if req.getRequestPath() != "/metrics" then
        req.setStatusCode(404)
        req.getOutputStream().close()
        return

      req.setStatusCode(200)
      req.getResponseHeaders().put(HttpString("content-type"), "text/plain")
      for m <- metrics do
        m.writeBytesTo(req.getOutputStream())
      req.getOutputStream().close()
  end handler

  def start(
    host: String = "[::]",
    port: Int = 10000,
    verbose: Boolean = false
  ): Undertow =
    if !verbose then MetricsServer.silenceJboss()
    val server = Undertow.builder()
      .addHttpListener(port, host)
      .setHandler(new BlockingHandler(handler))
      .setWorkerOption(
        org.xnio.Options.THREAD_DAEMON.asInstanceOf[org.xnio.Option[Any]],
        true
      )
      .build()
    server.start()
    server

object MetricsServer {

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
