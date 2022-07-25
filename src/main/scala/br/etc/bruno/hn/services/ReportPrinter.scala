package br.etc.bruno.hn.services

import br.etc.bruno.hn.services.Report.{ OverallCommentReport }

/**
 * Builds the final output as a tabular data
 * @param rows
 * @param topCommenter
 * @param div
 */
class ReportPrinter(rows: Seq[OverallCommentReport],
                    topCommenter: Int,
                    div: String = "|") {

  def tabularData(): String = {
    val newHeader = header(topCommenter)
    val newBody = prepareBody()
    val columnSizes = calculateColumnSizes(newHeader, newBody)

    def pad(row: Seq[String]): Seq[String] =
      row.zipWithIndex.map {
        case (col, idx) =>
          col.padTo(columnSizes(idx), ' ')
      }

    val headerPadded = pad(newHeader)
    val bodyPadded = newBody.map(pad)
    val line = Array.fill(columnSizes.sum + newHeader.length + 1)('-').mkString + "\n"

    line +
      headerPadded.mkString(div, div, div + "\n") +
      line +
      bodyPadded.map(_.mkString(div, div, div)).mkString("", "\n", "\n") +
      line
  }

  private def calculateColumnSizes(newHeader: Seq[String],
                                   newBody: Seq[Seq[String]]): Seq[Int] = {
    newBody.foldLeft(newHeader.map(_.length)) {
      case (sizes, row) =>
        row.zipWithIndex.foldLeft(sizes) {
          case (localSizes, (col, idx)) =>
            if (localSizes(idx) < col.length)
              localSizes.updated(idx, col.length)
            else
              localSizes
        }
    }
  }

  private def prepareBody(noData: String = "< no data >"): Seq[Seq[String]] = {
    rows map { row =>
      val newRow = row.prettyColumns
      if (newRow.size < topCommenter + 1) {
        newRow ++ Array.fill(topCommenter + 1 - newRow.length)(noData)
      } else
        newRow
    }
  }

  private def header(topCommenter: Int): Seq[String] =
    Seq("Story") ++ (1 to topCommenter).map(idx => s"#$idx Top Commenter")

}
