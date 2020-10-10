package ustats

import java.util.concurrent.atomic.DoubleAdder

/** Gauges may be increased and decreased. */
class Gauge(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Double) = adder.add(n)
  def -=(n: Double) = adder.add(-n)
  def set(n: Double) = {
    adder.reset()
    adder.add(n)
  }
}
