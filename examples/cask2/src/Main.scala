import java.util.concurrent.ConcurrentHashMap
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

// Extends this trait to time all routes.
trait Timed extends cask.Main {

  lazy val histograms = {
    // exhaustive list of paths
    val paths = for {
      routes <- allRoutes
      route <- routes.caskMetadata.value.map(x =>
        x: cask.router.EndpointMetadata[_]
      )
    } yield (route.endpoint.path)

    val cmap = new ConcurrentHashMap[String, ustats.Histogram]
    for (path <- paths) {
      cmap.put(
        path,
        ustats.namedHistogram("http_requests_seconds", "path" -> path)
      )
    }
    cmap
  }

  override def defaultHandler: BlockingHandler = {
    val parent = super.defaultHandler

    val timedHandler = new HttpHandler {
      def handleRequest(exchange: HttpServerExchange): Unit = {
        val histogram = histograms.get(exchange.getRequestPath())
        if (histogram != null) {
          histogram.time(
            parent.handleRequest(exchange)
          )
        } else {
          parent.handleRequest(exchange)
        }
      }
    }

    new BlockingHandler(timedHandler)
  }

}

object Main extends cask.MainRoutes with Timed {

  @cask.get("/")
  def index() = {
    cask.Response("here you are!", 200)
  }

  @cask.get("/metrics")
  def metrics() = ustats.metrics

  initialize()

}
