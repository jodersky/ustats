object Main extends App {

  var database = collection.mutable.ListBuffer.empty[String]

  val itemsTotal = ustats.gauge()

  ustats.probe(10){
    itemsTotal.set(database.length)
  }

  ustats.server.start("localhost", 8081)

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
}
