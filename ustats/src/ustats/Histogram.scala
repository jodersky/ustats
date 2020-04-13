package ustats

import java.util.concurrent.atomic.DoubleAdder
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class Histogram(
    buckets: Array[Double],
    adders: Array[DoubleAdder],
    count: DoubleAdder,
    sum: DoubleAdder
) {

  /** Record a single data point. */
  def add(value: Double): Unit = {
    var i = 0
    while (i < buckets.length) {
      if (value <= buckets(i)) {
        adders(i).add(1)
      }
      i += 1
    }
    count.add(1)
    sum.add(value)
  }

  /** Record the time in seconds it takes to run the given function. */
  def time[A](fct: => A): A = {
    val now = System.nanoTime()
    val result = fct
    val elapsedSeconds = (System.nanoTime - now) / 1e9
    add(elapsedSeconds)
    result
  }

  /** Record the time in seconds it takes to complete the given future. */
  def timeAsync[A](
      fct: => Future[A]
  )(implicit ec: ExecutionContext): Future[A] = {
    val now = System.nanoTime()
    val result = fct
    result.onComplete(_ => add((System.nanoTime - now) / 1e9))
    result
  }

}
