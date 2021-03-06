package io.prediction.commons.graphchi.itemrec

import breeze.linalg._

import scala.io.Source

object MatrixMarketReader {

  /* read dense matrix market from file and return DenseMatrix object */
  def readDense(path: String): DenseMatrix[Double] = {
    val matrixFile = Source.fromFile(path)
    // skip line starts with %
    // skip empty line
    val lines = matrixFile.getLines()
      .filter(line => (line.length != 0) && (!line.startsWith("%")))

    // first line is matrix size
    if (lines.hasNext) {
      val line = lines.next()
      val size = line.split("""\s+""")

      val (rowNum, colNum): (Int, Int) = try {
        (size(0).toInt, size(1).toInt)
      } catch {
        case e: Exception =>
          throw new RuntimeException(s"Cannot extract matrix size from the line: ${line}. ${e}")
      }

      println(s"${rowNum}, ${colNum}")
      val matrix = DenseMatrix.zeros[Double](rowNum, colNum)

      var r = 0
      var c = 0
      lines.foreach { line =>
        if (c >= colNum) {
          throw new RuntimeException(s"Number of elements greater than the defined size: ${rowNum} ${colNum}")
        } else {

          println(s"${r}, ${c} = ${line}")
          try {
            matrix(r, c) = line.toDouble
          } catch {
            case e: Exception =>
              throw new RuntimeException(s"Cannot convert line: ${line} to double. ${e}")
          }
          r += 1
          if (r == rowNum) {
            r = 0
            c += 1
          }
        }
      }
      // c must == colNum when finish
      if (c < colNum) {
        throw new RuntimeException(s"Number of elements smaller than the defined size: ${rowNum} ${colNum}")
      }
      println(matrix)
      matrix
    } else {
      DenseMatrix.zeros[Double](0, 0)
    }
  }

}