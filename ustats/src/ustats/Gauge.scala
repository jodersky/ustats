package ustats

import java.util.concurrent.atomic.DoubleAdder

class Gauge(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Int) = adder.add(n)
  def -=(n: Int) = adder.add(-n)
}
