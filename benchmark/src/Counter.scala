package ustats

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit}
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Scope

@State(Scope.Benchmark)
class state {
  val collector = new Stats()

  val foo = collector.counter()
  var x: Long = 0
}

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class TestCounter {

  @Benchmark
  def nativeCounter(state: state): Unit = state.x += 1

  @Benchmark
  def counter(state: state): Unit = state.foo += 1

  @Benchmark
  def metrics(state: state): Unit = state.collector.metrics


  //@Benchmark
  //def counter(state: state): Unit = state.foo += 1

}
