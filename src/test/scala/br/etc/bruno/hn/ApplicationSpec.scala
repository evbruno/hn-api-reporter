package br.etc.bruno.hn

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ApplicationSpec
  extends AnyFreeSpec
    with Matchers
    with OptionValues
    with MockAPI {

  implicit val svc = mockService

  "crawler should fetch Story and return all of its comments" in {
    val subject = new StoryCrawler()
    val story = subject.fetchStory(StoryID).get
    val comments = story.kids

    comments.size shouldBe 4
    // asserting all the valid IDs are here
    comments.keys.toSet shouldBe Set(27898719L, 27895824L, 27896396L, 27898570L)
  }

  "application should return the correct report" in {
    val subject = new Application(4, 2)
    val report = subject.run()

    report.size shouldBe 1
    report(0).prettyColumns.mkString(" | ") shouldBe
      """Thoughts on "The Theory and Craft of Digital Preservation" | mftb (2 for story - 2 total) | rambambram (1 for story - 1 total) | adultSwim (1 for story - 1 total)"""
  }

}
