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

  def add(value: Double): Unit = {
    for (i <- 0 until buckets.length) {
      if (value <= buckets(i)) {
        adders(i).add(1)
      }
    }
    count.add(1)
    sum.add(value)
  }

  def time[A](fct: => A): A = {
    val now = System.nanoTime()
    val result = fct
    val elapsedSeconds = (System.nanoTime - now) / 1e9
    add(elapsedSeconds)
    result
  }

  def timeAsync[A](
      fct: => Future[A]
  )(implicit ec: ExecutionContext): Future[A] = {
    val now = System.nanoTime()
    val result = fct
    result.onComplete(_ => add((System.nanoTime - now) / 1e9))
    result
  }

}
