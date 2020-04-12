package ustats

import java.util.concurrent.atomic.AtomicLongArray

class Gauge(data: AtomicLongArray, idx: Int) {
  def +=(n: Int) = data.addAndGet(idx, n)
  def -=(n: Int) = data.addAndGet(idx, -n)
}
