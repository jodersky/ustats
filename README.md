# μstats

[![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://crashbox.zulipchat.com/#narrow/stream/343726-ustats)
[![ustats Scala version support](https://index.scala-lang.org/jodersky/ustats/ustats/latest.svg)](https://index.scala-lang.org/jodersky/ustats/ustats)
[![stability: soft](https://img.shields.io/badge/stability-soft-white)](https://www.crashbox.io/stability.html)

A simple and intuitive metrics collection library.

## Getting Started

μstats is available from maven central for Scala 3.1 and above, for the JVM and
Scala Native. Add its coordinates to your build config:

- mill: `ivy"io.crashbox::ustats:<latest_version>"`
- sbt: `"io.crashbox" %% "ustats" % "<latest_version>"`

where `<latest_version>` is
[![latest_version](https://index.scala-lang.org/jodersky/ustats/ustats/latest.svg)](https://index.scala-lang.org/jodersky/ustats/ustats)

## Examples

Basic example, create a counter using the default collector:

```scala
val myCounter = ustats.global.counter("my_counter", "This is just a simple counter.")
myCounter += 1

println(ustats.global.metrics())

// # HELP my_counter This is just a simple counter.
// # TYPE my_counter counter
// my_counter 1.0
```

You can also add label-value pairs to individual metrics:

```scala
val myGauge = ustats.global.gauge("my_gauge", labels = Seq("label1" -> "foo", "label2" -> 42))
myGauge += 1
myGauge += 2

println(ustats.global.metrics())
// # TYPE my_counter gauge
// my_gauge{label1="foo", label2="42"} 3.0
```

However, you'd usually want to declare one metric sharing a common basename, and
add labels on demand:

```scala
val queueSizes = ustats.global.gauges("queue_size").labelled("queue_name")

queueSizes(queue_name = "queue1") += 10
queueSizes(queue_name = "queue1") -= 1
queueSizes(queue_name = "queue2") += 2

println(ustats.global.metrics())
// # TYPE queue_size gauge
// queue_size{queue_name="queue1"} 9.0
// queue_size{queue_name="queue2"} 2.0
```

User-defined grouping of metrics:

```scala
val mymetrics = ustats.Metrics()

val currentUsers = mymetrics.gauge("my_app_current_users")
currentUsers += 10
currentUsers -= 1
println(mymetrics.metrics())
```

## Probing

Sometimes it is useful to collect metrics in batch jobs. For example, querying
the number of entries in a database, or instrumenting some existing code without
modifying it. ustats has a builtin "probe" mechanism to run batch jobs
repeatedly at customizable intervals.

```scala
val counter1 = ustats.global.counter("counter1")
val gauge1 = ustats.global.gauge("gauge1")

// run this action every 10 seconds
ustats.global.probe("query_database", 10){
  // query database
  counter1 += 1
  gauge1.set(42)
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
ustats.server.global.start("localhost", 10000)

// custom server for custom stats
val metrics = ustats.Metrics()
val server = ustats.server.MetricsServer(metrics)
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
