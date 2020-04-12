package ustats

import java.util.concurrent.atomic.AtomicLongArray

class Counter(data: AtomicLongArray, idx: Int) {
  def +=(n: Int): Unit = {
    require(
      n >= 0,
      "negative argument given to += (counters may only be increased)"
    )
    data.addAndGet(idx, n)
  }
}
