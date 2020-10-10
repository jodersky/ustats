package ustats

import java.util.{concurrent => juc}

class ProbeFailedException(cause: Exception) extends Exception(cause)

object Probing {
  def makeThreadPool(n: Int) =
    juc.Executors.newScheduledThreadPool(
      n,
      new juc.ThreadFactory {
        override def newThread(r: Runnable) = {
          val t = new Thread(r)
          t.setDaemon(true)
          t.setName("ustats-probe")
          t
        }
      }
    )
}

trait Probing { this: Stats =>

  lazy val probePool: juc.ScheduledExecutorService = Probing.makeThreadPool(
    math.min(Runtime.getRuntime().availableProcessors(), 4)
  )

  // override this if you want to deactivate probe failure reporting
  lazy val probeFailureCounter: Option[Counter] = Some(
    namedCounter("ustats_probe_failures_count")
  )

  object probe {

    /** Run a given probing action regularly.
      *
      * Although an action can contain arbitrary code, it is intended to be used
      * to measure something and set various ustats metrics (counters, gauges,
      * histograms, etc) in its body.
      *
      * All actions are run in a dedicated thread pool.
      */
    def apply(
        rateInSeconds: Long,
        pool: juc.ScheduledExecutorService = probePool
    )(action: => Any): juc.ScheduledFuture[_] = {
      // initialize the lazy val, so that the metric is exposed even if there
      // are no failures
      probeFailureCounter
      pool.scheduleAtFixedRate(
        () =>
          try {
            action
          } catch {
            case ex: Exception =>
              probeFailureCounter.foreach(_ += 1)
              (new ProbeFailedException(ex)).printStackTrace()
          },
        0L,
        rateInSeconds,
        juc.TimeUnit.SECONDS
      )
    }

    /** Async wrapper for apply(). */
    def async(
        rateInSeconds: Long,
        pool: juc.ScheduledExecutorService = probePool
    )(action: => scala.concurrent.Future[_]): juc.ScheduledFuture[_] =
      apply(rateInSeconds, pool) {
        scala.concurrent.Await.result(
          action,
          scala.concurrent.duration.FiniteDuration(
            rateInSeconds,
            juc.TimeUnit.SECONDS
          )
        )
      }
  }

}
