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

  def labelify(baseName: String, labels: Seq[(String, Any)]): String =
    if (labels.isEmpty) {
      baseName
    } else {
      baseName + labels
        .map { case (key, value) => s"""$key="$value"""" }
        .mkString("{", ", ", "}")
    }

}
