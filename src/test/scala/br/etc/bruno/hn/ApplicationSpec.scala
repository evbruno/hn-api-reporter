package br.etc.bruno.hn

import br.etc.bruno.hn.HackerNewsAPI.{ Service }
import br.etc.bruno.hn.api._
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class ApplicationSpec extends AnyFreeSpec with Matchers with OptHelpers with OptionValues {

  val StoryID = 27880018L

  // mocking the service
  implicit val service: Service = new Service {

    override def fetchTopStories(): Option[TopStoriesResponse] =
      Some(TopStoriesResponse(Seq(StoryID)))

    // found a small story with few comments
    // https://news.ycombinator.com/item?id=27880018

    val cache: Map[Long, ItemResponse] = Map(
      27880018L -> ItemResponse(27880018, "story", "todsacerdoti", title = "Thoughts on \"The Theory and Craft of Digital Preservation\"", kids = Seq(27898719L, 27895824L)),
      27898719L -> ItemResponse(27898719, "comment", "adultSwim", parent = 27880018L),
      27895824L -> ItemResponse(27895824, "comment", "mftb", parent = 27880018L, kids = Seq(27896396L, 27898570L)),
      27896396L -> ItemResponse(27896396, "comment", "rambambram", parent = 27895824, kids = Seq(27900391L)),
      27900391L -> ItemResponse(27900391, "comment", parent = 27896396, deleted = true),
      27898570L -> ItemResponse(27898570, "comment", "mftb", parent = 27895824),
    )

    override def fetchItem(id: Long): Option[ItemResponse] =
      cache.get(id)
  }

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
