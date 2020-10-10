package ustats

import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.DoubleAdder

object Stats {
  def apply(prefix: String = "", BlockSize: Int = 32) =
    new Stats(prefix, BlockSize)
}

/** A memory-efficient, concurrent-friendly metrics collection interface.
  *
  * @param prefix A string that is prepended to all automatically generated
  *               metrics.
  * @param BlockSize Metrics are stored in a linked list of arrays ahich are
  *                  allocated on demand. The size of the arrays in each block
  *                  is controlled by this parameter. It does not need to be
  *                  changed for regular use, but can be tuned to a larger
  *                  value, should you have many metrics.
  */
class Stats(val prefix: String = "", BlockSize: Int = 32) {

  // For fast access and a light memory footprint, all data is stored as chunks
  // of contiguous DoubleAdder arrays. DoubleAdders (as opposed to AtomicDoubles)
  // are used to minimize contention when several threads update the same metric
  // concurrently.
  private class Block {
    @volatile var next: Block = null
    val metrics = new Array[String](BlockSize)
    val data = Array.fill[DoubleAdder](BlockSize)(new DoubleAdder)
    @volatile var i = 0 // next index to fill
  }
  private var curr = new Block
  private var head = curr

  /** Write metrics as they are right now to the given output stream.
    *
    * Concurrency note: this method does not block updating metrics while it is
    * called. This can lead to inconsistent reads where, while printing metrics
    * to the output stream, latter metrics are updated to a new value while an
    * old value from a previous metric has already been printed.
    * Nevertheless, these inconsistent reads offer increased performance and are
    * acceptable for two reasons:
    * 1) metrics may only ever be added; they can never be removed
    * 2) atomic reads are not important for the purpose of metrics collection
    */
  def writeMetricsTo(out: OutputStream): Unit = {
    var block = head
    while (block != null) {
      var i = 0
      while (i < block.i) {
        out.write(block.metrics(i).getBytes("utf-8"))
        out.write(32) // space
        out.write(block.data(i).sum().toString().getBytes("utf-8"))
        out.write(10) // newline
        i += 1
      }
      block = block.next
    }
    out.flush()
  }

  /** Show metrics as they are right now.
    *
    * See also writeMetricsTo() for a note on consistency.
    */
  def metrics: String = {
    val out = new ByteArrayOutputStream
    writeMetricsTo(out)
    out.close()
    out.toString("utf-8")
  }

  /** Add a metric manually.
    *
    * This is a low-level escape hatch that should be used only when no other
    * functions apply.
    */
  def addMetric(name: String): DoubleAdder = synchronized {
    if (curr.i >= BlockSize) {
      val b = new Block
      curr.next = b
      curr = b
      addMetric(name)
    } else {
      val i = curr.i
      curr.metrics(i) = name
      curr.i += 1
      curr.data(i)
    }
  }

  /** Create and register counter with a given name and labels.
    *
    * The final metrics name will be composed of the passed name plus any labels,
    * according to the prometheus syntax.
    */
  def namedCounter(name: String, labels: (String, Any)*): Counter = {
    val adder = addMetric(util.labelify(name, labels))
    new Counter(adder)
  }

  /** Create and register a counter whose name is automatically derived from the
    * val it is assigned to (plus a global prefix, if set).
    *
    * E.g.
    *
    * {{{
    * val myFirstStatistic: stats.Counter = stats.counter("foo" -> "bar")
    * }}}
    *
    * will create a new counter whose metrics will be exposed as
    * `my_first_statistic{foo="bar"}`
    */
  def counter(
      labels: (String, Any)*
  )(implicit name: sourcecode.Name): Counter = {
    namedCounter(util.snakify(prefix + name.value), labels: _*)
  }

  def namedGauge(name: String, labels: (String, Any)*): Gauge = {
    val adder = addMetric(util.labelify(name, labels))
    new Gauge(adder)
  }

  def gauge(labels: (String, Any)*)(implicit name: sourcecode.Name): Gauge = {
    namedGauge(util.snakify(prefix + name.value), labels: _*)
  }

  def namedHistogram(
      name: String,
      buckets: BucketDistribution,
      labels: (String, Any)*
  ): Histogram = {
    val buckets0 = buckets.buckets.sorted
    require(
      labels.forall(_._1 != "le"),
      "histograms may not contain the label \"le\""
    )
    require(buckets0.size >= 1, "historams must have at least one bucket")
    require(buckets0.forall(!_.isNaN()), "histograms may not have NaN buckets")

    val buckets1 = if (buckets0.last == Double.PositiveInfinity) {
      Array.from(buckets0)
    } else {
      Array.from(buckets0 ++ Seq(Double.PositiveInfinity))
    }
    val adders = buckets1.map { bucket =>
      val label = if (bucket.isPosInfinity) {
        "+Inf"
      } else {
        bucket
      }
      addMetric(util.labelify(name + "_bucket", labels ++ Seq("le" -> label)))
    }
    new Histogram(
      buckets1,
      adders,
      addMetric(util.labelify(name + "_count", labels)),
      addMetric(util.labelify(name + "_sum", labels))
    )
  }

  def namedHistogram(name: String, labels: (String, Any)*): Histogram =
    namedHistogram(name, BucketDistribution.Default, labels: _*)

  def histogram(buckets: BucketDistribution, labels: (String, Any)*)(
      implicit name: sourcecode.Name
  ) = {
    namedHistogram(util.snakify(prefix + name.value), buckets, labels: _*)
  }

  def histogram(labels: (String, Any)*)(implicit name: sourcecode.Name) = {
    namedHistogram(
      util.snakify(prefix + name.value),
      BucketDistribution.Default,
      labels: _*
    )
  }

  /** A pseudo-metric used to expose application information in labels.
    *
    * E.g.
    * {{{
    * info("version" -> "0.1.0", "commit" -> "deadbeef")
    * }}}
    * will create the metric
    * {{{
    * build_info{version="0.1.0", commit="deadbeef"} 1.0
    * }}}
    * Note that the actual value will always be 1.
    */
  def info(properties: (String, Any)*): Unit =
    addMetric(util.labelify("build_info", properties)).add(1)

}
