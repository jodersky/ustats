package ustats

object BucketDistribution {

  /** This distribution is well-suited for typical web requests, it ranges from
    * milliseconds to seconds.
    */
  val httpRequestDuration = Seq(.005, .01, .025, .05, .1, .25, .5, 1, 3, 10)

  def linear(start: Double, width: Double, count: Int) = {
    val buckets = new Array[Double](count)
    for (i <- 0 until count) {
      buckets(i) = start + i * width
    }
    buckets.toSeq
  }

  def exponential(start: Double, factor: Double, count: Int) = {
    val buckets = new Array[Double](count)
    for (i <- 0 until count) {
      buckets(i) = start + math.pow(factor, i.toDouble)
    }
    buckets.toSeq
  }

}
