package ustats

import java.util.concurrent.atomic.DoubleAdder

class Counter(private val adder: DoubleAdder) extends AnyVal {
  def +=(n: Double): Unit = {
    require(
      n >= 0,
      "negative argument given to += (counters may only be increased)"
    )
    adder.add(n)
  }
}
