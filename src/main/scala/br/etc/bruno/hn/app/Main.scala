package br.etc.bruno.hn.app

import br.etc.bruno.hn.{ Application, HackerNewsAPI, api }

object Main {

  val DIV = " | "

  def main(args: Array[String]): Unit = {
    val (topStories, topCommenter) =
      if (args.length == 2) (args(0).toInt, args(1).toInt)
      else (30, 2)

    // FIXME I'm using this delegate for now to control the response I want to see
    // remove it before submitting!!
    val delegateService = HackerNewsAPI.impl
    implicit val svc = new HackerNewsAPI.Service {

      override def fetchTopStories(): Option[api.TopStoriesResponse] =
        Some(api.TopStoriesResponse(Seq(27880018L, 27890992L, 27887434L)))

      override def fetchItem(id: Long): Option[api.ItemResponse] =
        delegateService.fetchItem(id)

    }

    val app = new Application(topCommenter, topStories)

    // TODO make nice tabular output
    println(app.header().mkString(DIV))

    app.run().foreach { row =>
      println(row.prettyColumns.mkString(DIV))
    }

  }

}
