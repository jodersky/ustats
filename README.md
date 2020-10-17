# μstats

A simple and intuitive metrics collection library for Prometheus.

## Highlights

- ease of use: *does what you want it to do without any setup ceremony*

- efficiency: *light memory footprint and extremely fast*

## Getting Started

ustats is available from maven central (for Scala 2.13 and Dotty). Add its
coordinates to your build config:

- mill: `ivy"io.crashbox::ustats:<latest_version>"`
- sbt: `"io.crashbox" %% "ustats" % "<latest_version>"`

where `<latest_version>` is
[![latest_version](https://index.scala-lang.org/jodersky/ustats/ustats/latest.svg)](https://index.scala-lang.org/jodersky/ustats/ustats)

## Examples

Basic example, create a counter using the default collector:

```scala
val myCounter = ustats.counter("my_counter", "This is just a simple counter.")
myCounter += 1

println(ustats.metrics())

// # HELP my_counter This is just a simple counter.
// # TYPE my_counter counter
// my_counter 1.0
```

You can also add label-value pairs to individual metrics:

```scala
val myGauge = ustats.gauge("my_gauge", labels = Seq("label1" -> "foo", "label2" -> 42))
myGauge += 1
myGauge += 2

println(ustats.metrics())
// # TYPE my_counter gauge
// my_gauge{label1="foo", label2="42"} 3.0
```

However, you'd usually want to declare one metric sharing a common basename, and
add labels on demand:

```scala
val queueSizes = ustats.gauges("queue_size", labels = Seq("queue_name"))

queueSizes.labelled("queue1") += 10
queueSizes.labelled("queue1") -= 1
queueSizes.labelled("queue2") += 2

println(ustats.metrics())
// # TYPE queue_size gauge
// queue_size{queue_name="queue1"} 9.0
// queue_size{queue_name="queue2"} 2.0
```

Use your own collector:

```scala
val collector = new ustats.Stats()

val currentUsers = collector.gauge("my_app_current_users")
currentUsers += 10
currentUsers -= 1
println(collector.metrics())
```

## Probing

Sometimes it is useful to collect metrics in batch jobs. For example, querying
the number of entries in a database, or instrumenting some existing code without
modifying it. ustats has a builtin "probe" mechanism to run batch jobs
repeatedly at customizable intervals.

```scala
val counter1 = ustats.counter("counter1")
val gauge1 = ustats.gauge("gauge1")

// run this action every 10 seconds
ustats.probe("query_database", 10){
  // query database
  counter1 += 1
  gauge1.set(42)
}

// also works with async code
ustats.probe.async("query_database", 10) { implicit ec =>
  val f: Future[_] = // something that returns a Future[_]
  f.map{ _ =>
    counter1 += 1
  }
}
```

Note that failures of probes themselves are recorded and exposed as a metric.

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
val stats = new ustats.Stats()
val server = new ustats.MetricsServer(stats)
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
mill benchmark.runJmh -wi 10 -i 10 -f 1 -t 1

Benchmark            Mode  Cnt    Score   Error  Units
TestCounter.counter  avgt   10    9.742 ± 0.074  ns/op
TestCounter.metrics  avgt   10  160.115 ± 9.994  ns/op

# This simulates heavy parallel access with 8 concurrent threads
mill benchmark.runJmh -wi 10 -i 10 -f 1 -t 8

Benchmark            Mode  Cnt    Score    Error  Units
TestCounter.counter  avgt   10   17.691 ±  1.050  ns/op
TestCounter.metrics  avgt   10  601.332 ± 12.522  ns/op
```
