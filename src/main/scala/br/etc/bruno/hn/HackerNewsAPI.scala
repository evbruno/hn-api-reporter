package br.etc.bruno.hn

import br.etc.bruno.hn.model.ID

object HackerNewsAPI {

  import api._

  val API_PREFIX = "https://hacker-news.firebaseio.com/v0/"

  // blocking version: it should be fine for now

  /**
   * Interface to fetch data from the API
   *
   * This impl is blocking/sync at this moment
   */
  trait Service {

    def fetchTopStories(): Option[TopStoriesResponse]

    def fetchItem(id: ID): Option[ItemResponse]

  }

  object impl extends Service {

    import sttp.client3.circe._
    import sttp.client3._
    import io.circe.generic.auto._

    //TODO consider moving to HttpClientFutureBackend
    private val backend = HttpClientSyncBackend()

    override def fetchTopStories(): Option[TopStoriesResponse] = {
      val request = basicRequest
        .get(uri"$API_PREFIX/topstories.json?print=pretty")
        .response(asJson[Array[Long]])

      val b = request.send(backend).body
      b.toOption.map(TopStoriesResponse(_))
    }

    override def fetchItem(id: Long): Option[ItemResponse] = {
      val request = basicRequest
        .get(uri"$API_PREFIX/item/$id.json?print=pretty")
        .response(asJson[ItemResponse])

      request.send(backend).body match {
        case Left(err) =>
          sys.error(err.getMessage)
          None
        case Right(value) =>
          Some(value)
      }
    }

  }

}




