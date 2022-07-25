package br.etc.bruno.hn

import br.etc.bruno.hn.model._
import br.etc.bruno.hn.services.Report.{ CommentReport, OverallCommentReport, OverallUserReport }
import br.etc.bruno.hn.services.{ Report, ReportPrinter }
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ReportingSpec extends AnyFreeSpec with Matchers {

  /**
   *
   * | Story A            | Story B             | Story C             |
   * |--------------------|---------------------|---------------------|
   * | user-a (1 comment) | user-a (4 comments) | user-a (4 comments) |
   * | user-b (2 comment) | user-b (3 comments) | user-b (5 comments) |
   * | user-c (3 comment) | user-c (2 comments) | user-c (3 comments) |
   *
   * ## output
   *
   * | Story   | 1st Top Commenter               | 2nd Top Commenter |
   * |---------|---------------------------------|---------------------------------|
   * | Story A | user-c (3 for story - 8 total)  | user-b (2 for story - 10 total) |
   */

  def c(id: ID, parent: ID, user: String): (ID, Comment) = {
    id -> Comment(
      id = id,
      parent = parent,
      user = s"user-$user"
    )
  }


  /* totals: 1a 2b 3c

             10,Story A
        11,a    12,c   15,b
                13,b   16,c
                14,c

  */
  val StoryA = Story(10, "Story A",
    Map(
      c(11, 10, "a"),
      c(12, 10, "c"),
      c(13, 12, "b"),
      c(14, 13, "c"),
      c(15, 10, "b"),
      c(16, 15, "c")
    )
  )

  /* totals: 4a 3b 2c
                  20,Story B
         21,a             22,a         23,b
     26,c 27,a 30,b   24,c 25,b 28,a  <empty>
  */
  val StoryB = Story(20, "Story B",
    Map(
      c(21, 20, "a"),
      c(26, 21, "c"),
      c(27, 21, "a"),
      c(30, 21, "b"),
      c(22, 20, "a"),
      c(24, 22, "c"),
      c(25, 22, "b"),
      c(28, 22, "a"),
      c(23, 20, "b")
    )
  )

  /* totals: 4a 5b 3c
                     30,StoryC
               31,a               32,a     41,b
        33,b           34,c
        35,c           36,b
      39,a 40,b    37,a 38,b 42,c

   */
  val StoryC = Story(30, "Story C",
    Map(
      c(31, 30, "a"),
      c(33, 31, "b"),
      c(35, 33, "c"),
      c(39, 35, "a"),
      c(40, 35, "b"),
      c(34, 30, "c"),
      c(36, 34, "b"),
      c(37, 36, "a"),
      c(38, 36, "b"),
      c(42, 36, "c"),
      c(32, 30, "a"),
      c(41, 30, "b")
    )
  )

  "story comments" - {
    "build tabular data output" in {
      val topCommenter = 4
      val rows = Seq(
        OverallCommentReport(StoryA, Seq(
          OverallUserReport(CommentReport("user-c", 3), 8),
          OverallUserReport(CommentReport("user-b", 2), 10),
          OverallUserReport(CommentReport("user-a", 1), 9)
        )),
        OverallCommentReport(StoryB, Seq(
          OverallUserReport(CommentReport("user-a", 4), 9),
          OverallUserReport(CommentReport("user-b", 3), 10),
          OverallUserReport(CommentReport("user-c", 2), 8)
        )),
        OverallCommentReport(StoryC, Seq(
          OverallUserReport(CommentReport("user-b", 5), 10),
          OverallUserReport(CommentReport("user-a", 4), 9),
          OverallUserReport(CommentReport("user-c", 3), 8)
        )),
      )
      val printer = new ReportPrinter(rows, topCommenter)
      val table = printer.tabularData()
      val expected = """|-------------------------------------------------------------------------------------------------------------------------
                        ||Story  |#1 Top Commenter               |#2 Top Commenter               |#3 Top Commenter              |#4 Top Commenter|
                        |-------------------------------------------------------------------------------------------------------------------------
                        ||Story A|user-c (3 for story - 8 total) |user-b (2 for story - 10 total)|user-a (1 for story - 9 total)|< no data >     |
                        ||Story B|user-a (4 for story - 9 total) |user-b (3 for story - 10 total)|user-c (2 for story - 8 total)|< no data >     |
                        ||Story C|user-b (5 for story - 10 total)|user-a (4 for story - 9 total) |user-c (3 for story - 8 total)|< no data >     |
                        |-------------------------------------------------------------------------------------------------------------------------
                        |""".stripMargin

      table shouldBe expected
    }
  }

}