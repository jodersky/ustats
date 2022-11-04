package ustats

import types.DoubleAdder

/** Gauges may be increased and decreased. */
class Gauge(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Double): Unit = adder.add(n)
  def +=(n: Float): Unit = this.+=(n.toDouble)
  def +=(n: Long): Unit = this.+=(n.toDouble)
  def +=(n: Int): Unit = this.+=(n.toDouble)
  def +=(n: Short): Unit = this.+=(n.toDouble)
  def +=(n: Byte): Unit = this.+=(n.toDouble)

  def -=(n: Double) = adder.add(-n)
  def -=(n: Float): Unit = this.-=(n.toDouble)
  def -=(n: Long): Unit = this.-=(n.toDouble)
  def -=(n: Int): Unit = this.-=(n.toDouble)
  def -=(n: Short): Unit = this.-=(n.toDouble)
  def -=(n: Byte): Unit = this.-=(n.toDouble)

  def set(n: Double): Unit = {
    adder.reset()
    adder.add(n)
  }
  def set(n: Float): Unit = this.set(n.toDouble)
  def set(n: Long): Unit = this.set(n.toDouble)
  def set(n: Int): Unit = this.set(n.toDouble)
  def set(n: Short): Unit = this.set(n.toDouble)
  def set(n: Byte): Unit = this.set(n.toDouble)

  def reset(): Unit = adder.reset()
  def inc(): Unit = adder.add(1.0)
  def dec(): Unit = adder.add(-1.0)
}
