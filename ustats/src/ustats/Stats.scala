package ustats

import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.DoubleAdder

/** A memory-efficient, concurrent-friendly metrics collection interface.
  *
  * @param BlockSize Metrics are stored in a linked list of arrays which are
  *                  allocated on demand. The size of the arrays in each block
  *                  is controlled by this parameter. It does not need to be
  *                  changed for regular use, but can be tuned to a larger
  *                  value, should you have many metrics.
  */
class Stats(BlockSize: Int = 128) {

  // For fast access and a light memory footprint, all data is stored as chunks
  // of names and DoubleAdder arrays. DoubleAdders (as opposed to AtomicDoubles)
  // are used to minimize contention when several threads update the same metric
  // concurrently.
  private class Block {
    @volatile var next: Block = null
    val metrics = new Array[String](BlockSize) // name
    val data = Array.fill[DoubleAdder](BlockSize)(new DoubleAdder)
    @volatile var i = 0 // next index to fill
  }
  private var curr = new Block
  private var head = curr

  private val infos = new ConcurrentLinkedDeque[String]

  /** Write metrics as they are right now to the given output stream.
    *
    * Concurrency note: this method does not block updating or adding new
    * metrics while it is called.
    */
  def writeMetricsTo(out: OutputStream, includeInfo: Boolean = true): Unit = {
    if (includeInfo) {
      val items = infos.iterator()
      while (items.hasNext()) {
        out.write(items.next().getBytes("utf-8"))
      }
    }
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

  /** Show metrics as they are right now. */
  def metrics(includeInfo: Boolean = true): String = {
    val out = new ByteArrayOutputStream
    writeMetricsTo(out, includeInfo)
    out.close()
    out.toString("utf-8")
  }

  /** Add an informational comment, to be exported when metrics are read. */
  def addInfo(
      name: String,
      help: String = null,
      tpe: String = null,
      comment: String = null
  ): String = {
    def escape(line: String) =
      line
        .replace("""\""", """\\""")
        .replace("\n", """\n""")

    val commentBuilder = new StringBuilder
    if (comment != null) {
      commentBuilder ++= "# "
      commentBuilder ++= escape(comment)
      commentBuilder ++= "\n"
    }
    if (help != null) {
      commentBuilder ++= "# HELP "
      commentBuilder ++= name
      commentBuilder ++= " "
      commentBuilder ++= escape(help)
      commentBuilder ++= "\n"
    }
    if (tpe != null) {
      commentBuilder ++= "# TYPE "
      commentBuilder ++= name
      commentBuilder ++= " "
      commentBuilder ++= escape(tpe)
      commentBuilder ++= "\n"
    }

    val info = commentBuilder.result()
    infos.add(info)
    info
  }

  /** Add a metric manually.
    *
    * This is a low-level escape hatch that should be used only when no other
    * functions apply.
    */
  def addRawMetric(
      name: String,
      labels: Seq[(String, Any)]
  ): DoubleAdder = synchronized {
    if (curr.i >= BlockSize) {
      val b = new Block
      curr.next = b
      curr = b
      addRawMetric(name, labels)
    } else {
      val i = curr.i
      curr.metrics(i) = util.labelify(name, labels)
      curr.i += 1
      curr.data(i)
    }
  }

  private def addRawCounter(
      name: String,
      labels: Seq[(String, Any)] = Nil
  ): Counter =
    new Counter(addRawMetric(name, labels))

  private def addRawGauge(
      name: String,
      labels: Seq[(String, Any)] = Nil
  ): Gauge =
    new Gauge(addRawMetric(name, labels))

  protected def addRawHistogram(
      name: String,
      buckets: Seq[Double] = BucketDistribution.httpRequestDuration,
      labels: Seq[(String, Any)] = Nil
  ): Histogram = {
    val buckets0: Seq[Double] = buckets.sorted
    require(
      labels.forall(_._1 != "le"),
      "histograms may not contain the label \"le\""
    )
    require(buckets0.size >= 1, "histograms must have at least one bucket")
    require(buckets0.forall(!_.isNaN()), "histograms may not have NaN buckets")

    val buckets1 = if (buckets0.last == Double.PositiveInfinity) {
      Array.from(buckets0)
    } else {
      Array.from(buckets0 ++ Seq(Double.PositiveInfinity))
    }

    val adders = buckets1.zipWithIndex.map {
      case (bucket, idx) =>
        val label = if (bucket.isPosInfinity) {
          "+Inf"
        } else {
          bucket
        }

        addRawMetric(name + "_bucket", labels ++ Seq("le" -> label))
    }

    new Histogram(
      buckets1,
      adders,
      addRawMetric(name + "_count", labels),
      addRawMetric(name + "_sum", labels)
    )
  }

  /** Add a single counter with the given name and label-value pairs. */
  def counter(
      name: String,
      help: String = null,
      labels: Seq[(String, Any)] = Nil
  ): Counter = {
    addInfo(name, help, "counter")
    addRawCounter(name, labels)
  }

  /** Add a single gauge with the given name and label-value pairs. */
  def gauge(
      name: String,
      help: String = null,
      labels: Seq[(String, Any)] = Nil
  ): Gauge = {
    addInfo(name, help, "gauge")
    addRawGauge(name, labels)
  }

  /** Add a single histogram with the given name and label-value pairs. */
  def histogram(
      name: String,
      help: String = null,
      buckets: Seq[Double] = BucketDistribution.httpRequestDuration,
      labels: Seq[(String, Any)] = Nil
  ): Histogram = {
    addInfo(name, help, "histogram")
    addRawHistogram(name, buckets, labels)
  }

  /** Add a group of counters with the given name. */
  def counters(
      name: String,
      help: String = null,
      labels: Seq[String] = Nil
  ): Metrics[Counter] = {
    addInfo(name, help, "counter")
    new Metrics[Counter](
      labels.length,
      values => addRawCounter(name, labels.zip(values))
    )
  }

  /** Add a group of gauges with the given name. */
  def gauges(
      name: String,
      help: String = null,
      labels: Seq[String] = Nil
  ): Metrics[Gauge] = {
    addInfo(name, help, "gauge")
    new Metrics[Gauge](
      labels.length,
      values => addRawGauge(name, labels.zip(values))
    )
  }

  /** Add a group of histograms with the given name. */
  def histograms(
      name: String,
      help: String = null,
      buckets: Seq[Double] = BucketDistribution.httpRequestDuration,
      labels: Seq[String] = Nil
  ): Metrics[Histogram] = {
    addInfo(name, help, "histogram")
    new Metrics[Histogram](
      labels.length,
      values => addRawHistogram(name, buckets, labels.zip(values))
    )
  }

  /** A pseudo-metric used to expose application information in labels.
    *
    * E.g.
    * {{{
    * buildInfo("version" -> "0.1.0", "commit" -> "deadbeef")
    * }}}
    * will create the metric
    * {{{
    * build_info{version="0.1.0", commit="deadbeef"} 1.0
    * }}}
    * Note that the actual value will always be 1.
    */
  def buildInfo(properties: (String, Any)*): Unit =
    addRawMetric("build_info", properties).add(1)

}
