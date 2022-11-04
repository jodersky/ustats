package ustats

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit, Scope, State}

@State(Scope.Benchmark)
class state {
  val collector = new Metrics()

  val counter = collector.counter("counter")
}

@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class TestCounter {

  @Benchmark
  def counter(state: state): Unit = state.counter += 1

  @Benchmark
  def metrics(state: state): Unit = state.collector.metrics()

}
