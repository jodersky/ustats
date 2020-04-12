package ustats

trait BucketDistribution {
  def buckets: Seq[Double]
}
object BucketDistribution {

  /** The default distribution used by histogram() overloads that do not take a
    * distribution as argument.
    *
    * This distribution is well-suited for typical web requests, it ranges from
    * milliseconds to seconds.
    */
  object Default extends BucketDistribution {
    val buckets =
      Seq(.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10)
  }

  case class Linear(start: Double, width: Double, count: Int)
      extends BucketDistribution {
    val buckets: Seq[Double] = {
      val buckets = new Array[Double](count)
      for (i <- 0 until count) {
        buckets(i) = start + i * width
      }
      buckets
    }
  }
  case class Exponential(start: Double, factor: Double, count: Int)
      extends BucketDistribution {
    val buckets: Seq[Double] = {
      val buckets = new Array[Double](count)
      for (i <- 0 until count) {
        buckets(i) = start + math.pow(factor, i)
      }
      buckets
    }
  }

  implicit class Enumerated[N](bs: Seq[N])(implicit numeric: Numeric[N])
      extends BucketDistribution {
    def buckets = bs.map(b => numeric.toDouble(b))
  }

}
