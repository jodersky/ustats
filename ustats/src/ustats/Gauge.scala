package ustats

import java.util.concurrent.atomic.DoubleAdder

/** Gauges may be increased and decreased. */
class Gauge(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Int) = adder.add(n)
  def -=(n: Int) = adder.add(-n)
}
