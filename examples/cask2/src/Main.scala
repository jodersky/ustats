import java.util.concurrent.ConcurrentHashMap
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

// Extend this trait to time all routes.
trait Timed extends cask.Main {

  lazy val histograms = {
    val hs = ustats.histograms("http_requests_seconds", labels = Seq("method", "path"))

    // prepopulate histogram with all known routes
    for {
      routes <- allRoutes
      route <- routes.caskMetadata.value.map(x =>
        x: cask.router.EndpointMetadata[_]
      )
      m <- route.endpoint.methods
    } hs.labelled(m, route.endpoint.path)

    hs
  }

  override def defaultHandler: BlockingHandler = {
    val parent = super.defaultHandler

    val timedHandler = new HttpHandler {
      def handleRequest(exchange: HttpServerExchange): Unit = {
        val effectiveMethod = exchange.getRequestMethod.toString.toLowerCase()
        val endpoint = routeTries(effectiveMethod).lookup(cask.internal.Util.splitPath(exchange.getRequestPath).toList, Map())

        endpoint match {
          case None => parent.handleRequest(exchange)
          case Some(((_,metadata), _, _)) =>
            histograms.labelled(effectiveMethod, metadata.endpoint.path).time(
              parent.handleRequest(exchange)
            )
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

  @cask.get("/foo/:id/bar")
  def getFoo(id: String) = {
    cask.Response(s"foo is $id", 200)
  }

  @cask.post("/foo/:id/bar")
  def setFoo(id: String) = {
    cask.Response(s"foo is $id", 200)
  }

  @cask.get("/metrics")
  def metrics() = ustats.metrics()

  initialize()

}
