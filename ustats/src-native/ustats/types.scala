package ustats

object types:

  final type DoubleAdder = NativeDoubleAdder
  final type ConcurrentHashMap[K, V] = NativeConcurrentHashMap[K, V]
  final type ConcurrentLinkedDeque[A] = NativeConcurrentLinkedDeque[A]

  class NativeDoubleAdder:
    private var value: Double = 0
    def add(v: Double) = value += v
    def reset() = value = 0
    def sum(): Double = value

  class NativeConcurrentHashMap[K, V]:
    private val data = collection.mutable.Map.empty[K, V]

    def computeIfAbsent(k: K, f: K => V): V =
      data.getOrElseUpdate(k, f(k))

    def get(k: K): V | Null = data.get(k) match
      case None => null
      case Some(v) => v

  class NativeConcurrentLinkedDeque[A]:
    private val queue = collection.mutable.Queue.empty[A]
    def iterator() = queue.iterator
    def add(a: A) = queue.append(a)
