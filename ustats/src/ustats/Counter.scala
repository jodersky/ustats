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
}
