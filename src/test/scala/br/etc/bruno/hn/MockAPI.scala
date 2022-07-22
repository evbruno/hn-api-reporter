package br.etc.bruno.hn

import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.api.{ ItemResponse, TopStoriesResponse }

trait MockAPI extends OptHelpers {

  val TopStories: Seq[Long] = Seq(27880018L, 27888626L)

  // Used direct w/ the Comments
  val Kids: Map[Long, Seq[Long]] = Map(
    27880018L -> Seq(27898719L, 27895824L),
    27888626L -> Seq()
  )

  // mocking the service
  val mockService = new Service {

    override def fetchTopStories(): Option[TopStoriesResponse] =
      Some(TopStoriesResponse(TopStories))

    // some Stories with few comments
    // https://news.ycombinator.com/item?id=27880018
    // https://news.ycombinator.com/item?id=27888626

    val cache: Map[Long, ItemResponse] = Map(
      // story: 27880018L
      27880018L -> ItemResponse(27880018L, "story", "todsacerdoti", title = "Thoughts on \"The Theory and Craft of Digital Preservation\"", kids = Seq(27898719L, 27895824L)),
      27898719L -> ItemResponse(27898719L, "comment", "adultSwim", parent = 27880018L),
      27895824L -> ItemResponse(27895824L, "comment", "mftb", parent = 27880018L, kids = Seq(27896396L, 27898570L)),
      27896396L -> ItemResponse(27896396L, "comment", "rambambram", parent = 27895824L, kids = Seq(27900391L)),
      27900391L -> ItemResponse(27900391L, "comment", parent = 27896396L, deleted = true),
      27898570L -> ItemResponse(27898570L, "comment", "mftb", parent = 27895824L),
      // story: 27888626L
      27888626L -> ItemResponse(27888626L, "story", "Lammy", title = "Excuse Me… Some Digital Preservation Fallacies? (2006)", kids = Seq(27890299L, 27889132L)),
      27890299L -> ItemResponse(27890299L, "comment", "kmeisthax", parent = 27888626L, kids = Seq(27890724L)),
      27890724L -> ItemResponse(27890724L, "comment", "TazeTSchnitzel", parent = 27890299L, kids = Seq(27899552L)),
      27899552L -> ItemResponse(27899552L, "comment", "kmeisthax", parent = 27890724L, kids = Seq(27919417L)),
      27919417L -> ItemResponse(27919417L, "comment", "EricE", parent = 27899552L),
      27889132L -> ItemResponse(27889132L, "comment", "Jiro", parent = 27888626L),
    )

    override def fetchItem(id: Long): Option[ItemResponse] =
      cache.get(id)
  }

}
