package ustats

import java.util.{concurrent => juc}

class ProbeFailedException(cause: Exception) extends Exception(cause)

object Probing {
  def makeThreadPool(n: Int) = {
    val counter = new juc.atomic.AtomicInteger(0)
    juc.Executors.newScheduledThreadPool(
      n,
      new juc.ThreadFactory {
        override def newThread(r: Runnable) = {
          val t = new Thread(r)
          t.setDaemon(true)
          t.setName(s"ustats-probe-${counter.incrementAndGet()}")
          t
        }
      }
    )
  }
}

trait Probing { this: Stats =>

  lazy val probePool: juc.ScheduledExecutorService = Probing.makeThreadPool(
    math.min(Runtime.getRuntime().availableProcessors(), 4)
  )

  // override this if you want to deactivate probe failure reporting
  lazy val probeFailureCounter: Option[Metrics[Counter]] = Some(
    ustats.counters("ustats_probe_failures_count", labels = Seq("probe"))
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
        name: String,
        rateInSeconds: Long,
        pool: juc.ScheduledExecutorService = probePool
    )(action: => Any): juc.ScheduledFuture[_] = {
      pool.scheduleAtFixedRate(
        () =>
          try {
            action
          } catch {
            case ex: Exception =>
              probeFailureCounter.foreach { counter => counter(name).inc() }
              (new ProbeFailedException(ex)).printStackTrace()
          },
        0L,
        rateInSeconds,
        juc.TimeUnit.SECONDS
      )
    }

    /** Async wrapper for apply(). */
    def async(
        name: String,
        rateInSeconds: Long,
        pool: juc.ScheduledExecutorService = probePool
    )(
        action: scala.concurrent.ExecutionContext => scala.concurrent.Future[_]
    ): juc.ScheduledFuture[_] = {
      val ec = scala.concurrent.ExecutionContext.fromExecutorService(pool)
      apply(name, rateInSeconds, pool) {
        scala.concurrent.Await.result(
          action(ec),
          scala.concurrent.duration.FiniteDuration(
            rateInSeconds,
            juc.TimeUnit.SECONDS
          )
        )
      }
    }
  }

}
