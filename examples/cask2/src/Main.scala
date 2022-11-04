import java.util.concurrent.ConcurrentHashMap
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

// Extend this trait to time all routes.
trait Timed extends cask.Main {

  lazy val histograms = {
    val hs = ustats.global.histograms("http_requests_seconds").labelled("method", "path")

    // prepopulate histogram with all known routes
    for {
      routes <- allRoutes
      route <- routes.caskMetadata.value.map(x =>
        x: cask.router.EndpointMetadata[_]
      )
      m <- route.endpoint.methods
    } hs(m, route.endpoint.path)

    hs
  }

  override def defaultHandler: BlockingHandler = {
    val parent = super.defaultHandler

    val timedHandler = new HttpHandler {
      def handleRequest(exchange: HttpServerExchange): Unit = {
        val effectiveMethod = exchange.getRequestMethod.toString.toLowerCase()
        val endpoint = dispatchTrie.lookup(cask.internal.Util.splitPath(exchange.getRequestPath).toList, Map())
        endpoint match
          case None =>
            parent.handleRequest(exchange)
          case Some((methodMap, routeBindings, remaining)) =>
            methodMap.get(effectiveMethod) match
              case None => parent.handleRequest(exchange)
              case Some((routes, metadata)) =>
                histograms(method = effectiveMethod, path = metadata.endpoint.path).time(
                  parent.handleRequest(exchange)
                )
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
  def metrics() = ustats.global.metrics()

  initialize()

}
