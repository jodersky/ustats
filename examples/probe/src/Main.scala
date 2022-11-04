@main
def main() =
  var database = collection.mutable.ListBuffer.empty[String]

  val itemsTotal = ustats.global.gauge("items_total")

  ustats.global.probe("query_database", 10){
    val l = synchronized {database.length}
    if (l > 5) sys.error("random failure")
    itemsTotal.set(l)
  }

  ustats.server.global.start("localhost", 8081)

  println("Go to http://localhost:8081/metrics to see current metrics.")
  println("Ctrl+D to exit")

  var done = false
  while(!done){
    println("items: " + database.mkString(", "))
    print("add: ")
    val s = scala.io.StdIn.readLine()
    if (s == null) done = true
    else database += s
  }

  println("bye")
