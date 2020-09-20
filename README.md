# μstats

A simple and intuitive metrics collection library for Scala, to be used with
Prometheus.

## Highlights

- ease of use: *does what you want it to do without any setup ceremony*

- efficiency: *light memory footprint and extremely fast, optimized for
  concurrent systems*

## Getting Started

ustats is available from maven central (for Scala 2.13). Add its coordinates to
your build config:

- mill: `ivy"io.crashbox::ustats:<latest_version>"`
- sbt: `"io.crashbox" %% "ustats" % "<latest_version>"`

where `<latest_version>` is
[![latest_version](https://index.scala-lang.org/jodersky/ustats/ustats/latest.svg)](https://index.scala-lang.org/jodersky/ustats/ustats)

## Examples

Basic example, create a counter using the default collector:

```scala
val myCounter = ustats.counter()
myCounter += 1

println(ustats.metrics) // = my_counter 1.0
```

```scala
val myCounter = ustats.counter("label1" -> "foo", "label2" -> 42)
myCounter += 1
myCounter += 2

println(ustats.metrics) // = my_counter{label1="foo", label2="42"} 3.0
```

Use your own collector with a prefix:

```scala
val collector = ustats.Stats(previx = "my_app_")

val currentUsers = collector.gauge()
currentUsers += 10
currentUsers -= 1
println(collector.metrics) // = my_app_current_users 9.0
```

Override the name of a metric:

```scala
val myCounter = ustats.namedCounter("my_app_http_requests_total")
println(ustats.metrics) // = my_app_http_requests_total 0.0
```

Cask HTTP request timing:

```scala
object Main extends cask.MainRoutes {

  val httpRequests = ustats.histogram("path" -> "/index")

  @cask.get("/index")
  def index() = httpRequests.time {
    // do something
    cask.Response("here you are", 200)
  }

  @cask.get("/metrics")
  def metrics() = ustats.metrics

  initialize()

}
```

## Server

ustats includes an optional server module which allows you to export metrics
over HTTP, under the standard `/metrics` endpoint. The server module is based on
[undertow](https://github.com/undertow-io/undertow).

- mill: `ivy"io.crashbox::ustats-server:<latest_version>"`
- sbt: `"io.crashbox" %% "ustats-server" % "<latest_version>"`

```scala
// global server for global stats
ustats.server.start("localhost", 10000)

// custom server for custom stats
val stats = ustats.Stats()
val server = ustats.MetricsServer(stats)
server.start("localhost", 10000)
```

## Benchmarks

Since metrics may be updated frequently and by multiple concurrent threads, it
is imperative that updates be fast and avoid contention as much as possible.
ustats achieves this by using `java.util.concurrent.atomic.DoubleAdder`s to
store all metrics.

Here are some benchmarks obtained on a laptop with an Intel Core i7-8550U CPU,
1.80GHz base frequency with 4GHz turbo, 4 cores / 8 threads.

```
# Single threaded, ideal conditions
mill benchmark.runJmh -wi 20 -i 20 -f 1 -t 1

Benchmark            Mode  Cnt    Score   Error  Units
TestCounter.counter  avgt   20    9.905 ± 0.062  ns/op
TestCounter.metrics  avgt   20  113.145 ± 2.054  ns/op

# This simulates heavy parallel access with 64 concurrent threads
mill benchmark.runJmh -wi 20 -i 20 -f 1 -t 64

Benchmark            Mode  Cnt     Score     Error  Units
TestCounter.counter  avgt   20   156.438 ±   3.749  ns/op
TestCounter.metrics  avgt   20  3860.640 ± 138.861  ns/op
```

As shown, millions of updates per second are possible on consumer-grade
hardware, even with heavy concurrent access. Also, tens of thousands of metric
exports per second are possible (although metrics are typically read much less
frequently, usually once every few seconds).
