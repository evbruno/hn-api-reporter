package br.etc.bruno.hn

import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.api.{ ItemResponse, TopStoriesResponse }

trait MockAPI extends OptHelpers {

  val StoryID = 27880018L

  val Kids = Seq(27898719L, 27895824L)

  // mocking the service
  val mockService = new Service {

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

}
