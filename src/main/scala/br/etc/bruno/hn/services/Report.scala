package br.etc.bruno.hn.services

import br.etc.bruno.hn.model.Story

object Report {

  /**
   * Holds the number of comments for an <code>user</code>
   *
   * @param user
   * @param comments
   */
  case class CommentReport(user: String, comments: Int) {
    override def toString: String = s"($user, $comments)"

    def +(num: Int): CommentReport =
      copy(comments = comments + num)
  }

  /**
   * Holds a "row" for the final report for a single user, with the total comments + story comments
   *
   * @param report
   * @param total
   */
  case class OverallUserReport(report: CommentReport, total: Int)

  /**
   * Holds a "row" for the final report for the whole Story
   *
   * @param story
   * @param report
   */
  case class OverallCommentReport(story: Story, report: Seq[OverallUserReport]) {

    /**
     * eg:
     * | Story A | user-c (3 for story - 8 total)  | user-b (2 for story - 10 total) |
     *
     * @return columns with formatted string ready to be presented
     */
    def prettyColumns: Seq[String] = {
      Seq(story.title) ++ report.map {
        case OverallUserReport(r, total) =>
          s"${r.user} (${r.comments} for story - $total total)"
      }
    }
  }

  /**
   * Calculates all the "top commenters" for the input stories
   *
   * @param reportPerStory
   * @param topCommenter
   * @return
   */
  def aggregate(reportPerStory: Set[(Story, Set[CommentReport])], topCommenter: Int = 2): Seq[OverallCommentReport] = {
    val reportSeq = reportPerStory.toSeq.map { case(s, rs) => (s, rs.toSeq) }
    val userTotalsCache = sumComments(reportSeq)

    reportSeq map {
      case (story, report) =>
        // sort reverse and grab topCommenters
        val localUsers = report.sortBy(_.comments * -1).take(topCommenter)

        val userReports = localUsers map { report =>
          OverallUserReport(report, userTotalsCache(report.user))
        }

        OverallCommentReport(story, userReports)
    }
  }

  /**
   *
   * @param reportPerStory
   * @return sums the number of comments that user has made over all the Stories
   */
  private def sumComments(reportPerStory: Seq[(Story, Seq[CommentReport])]): Map[String, Int] = {
    reportPerStory.foldLeft(Map[String, Int]()) {
      case (acc, (_, storyReport)) =>
        storyReport.foldLeft(acc) {
          case (acc1, CommentReport(user, total)) =>
            acc1.updatedWith(user) {
              case Some(comments) => Some(total + comments)
              case None           => Some(total)
            }
        }
    }
  }

}
