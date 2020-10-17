package ustats

import java.util.concurrent.atomic.DoubleAdder

/** A counter may only ever be increased (or reset when the application restarts). */
class Counter(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Double): Unit = {
    require(
      n >= 0,
      "negative argument given to += (counters may only be increased)"
    )
    adder.add(n)
  }
  def +=(n: Float): Unit = this.+=(n.toDouble)
  def +=(n: Long): Unit = this.+=(n.toDouble)
  def +=(n: Int): Unit = this.+=(n.toDouble)
  def +=(n: Short): Unit = this.+=(n.toDouble)
  def +=(n: Byte): Unit = this.+=(n.toDouble)

  def inc() = adder.add(1.0)
}
