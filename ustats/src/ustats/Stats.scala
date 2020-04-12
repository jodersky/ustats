package ustats

import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicIntegerArray
import java.io.OutputStream
import java.io.ByteArrayOutputStream

/** A memory-efficient, concurrent-friendly metrics collection interface.
  *
  * @param prefix A string that is prepended to all automatically generated
  *               metrics.
  * @param BlockSize Metrics are stored in a linked list of arrays ahich are
  *                  allocated on demand. The size of the arrays in each block
  *                  is controlled by this parameter. It does not need to be
  *                  changed in regular usage, but can be tuned to a larger
  *                  value, should you have lots of metrics.
  */
class Stats(prefix: String = "", BlockSize: Int = 32) {

  // For fast access and a leightweight memory footprint, all data is stored
  // as an atomic collection rather than a collection of atomics.
  // Since the atomic collection is an array, it must have a fixed size. However,
  // metrics may be added on-the-fly at runtime, hence the array is logically
  // split into a linked list of blocks that are added on demand.
  private class Block {
    @volatile var next: Block = null
    val metrics = new Array[String](BlockSize)
    val data = new AtomicLongArray(new Array[Long](BlockSize))
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
    * Nevertheless, these inconsistent reads offer oncreased performance and are
    * acceptable for two reasons:
    * 1) metrics may only ever be added; they can never be removed
    * 2) atomic reads are not important for the purpose of metrics collection
    */
  def writeMetricsTo(out: OutputStream): Unit = { // not synchronized
    var block = head
    while (block != null) {
      var i = 0
      while (i < block.i) {
        out.write(block.metrics(i).getBytes("utf-8"))
        out.write(32) // space
        out.write(block.data.get(i).toString().getBytes("utf-8"))
        out.write(10) // newline
        i += 1
      }
      block = block.next
    }
    out.flush()
  }

  /** Get metrics as they are right now.
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
    * This is a low-level escape hatch that should be used only is no other
    * functions apply.
    */
  def addMetric(name: String): (AtomicLongArray, Int) = synchronized {
    if (curr.i >= BlockSize) {
      val b = new Block
      curr.next = b
      curr = b
      addMetric(name)
    } else {
      val i = curr.i
      curr.metrics(i) = name
      curr.i += 1
      (curr.data, i)
    }
  }

  /** Create and register counter with a given name and labels.
    *
    * The final metrics name will be composed of the passed name plus any labels,
    * according to the promentheus syntax.
    */
  def namedCounter(name: String, labels: (String, Any)*): Counter = {
    val (arr, idx) = addMetric(util.labelify(name, labels))
    new Counter(arr, idx)
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
    val (arr, idx) = addMetric(util.labelify(name, labels))
    new Gauge(arr, idx)
  }

  def gauge(labels: (String, Any)*)(implicit name: sourcecode.Name): Gauge = {
    namedGauge(prefix + name.value, labels: _*)
  }

}
