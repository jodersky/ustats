package ustats

import java.util.concurrent.ConcurrentHashMap

class Metrics[A](size: Int, mkNew: Seq[Any] => A) {
  private val all = new ConcurrentHashMap[Seq[Any], A]

  /** Return a metric only if it has already been added.
    *
    * Compared to `labelled()`, this method does not create a new metric if it
    * does not already exist. This is useful in situations where metrics can
    * be pre-populated, and can help avoid accidental cardinality explosion.
    */
  def existing(values: Any*): Option[A] = Option(all.get(values))

  /** Create or find an existing metric with the given label values. */
  def labelled(values: Any*): A = {
    require(values.size == size, "label size mismatch")
    all.computeIfAbsent(values, values => mkNew(values))
  }

  def apply(values: Any*): A = labelled(values: _*)
}
