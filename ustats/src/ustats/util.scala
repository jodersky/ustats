package ustats

object util {

  def snakify(camelCase: String): String = {
    val snake = new StringBuilder
    var prevIsLower = false
    for (c <- camelCase) {
      if (prevIsLower && c.isUpper) {
        snake += '_'
      }
      snake += c.toLower
      prevIsLower = c.isLower
    }
    snake.result()
  }

  def labelify(baseName: String, labels: Iterable[(String, Any)]): String =
    if (labels.isEmpty) {
      baseName
    } else {
      val it = labels.iterator
      val b = new StringBuilder
      b ++= baseName
      b += '{'

      val (key, value) = it.next()
      b ++= key
      b ++= "=\""
      b ++= value.toString
      b += '"'
      while it.hasNext do
        val (key, value) = it.next()
        b ++= ", "
        b ++= key
        b ++= "=\""
        b ++= value.toString
        b += '"'
      b += '}'
      b.result()
    }

}
