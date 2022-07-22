package br.etc.bruno.hn

import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.Report.OverallCommentReport
import br.etc.bruno.hn.model.Story
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class Application(topCommenter: Int, topStories: Int)(implicit val service: Service) {

  // the global one is fine for now, maybe a custom threadpool / ec
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val timeout = 10.minutes

  def header(): Seq[String] =
    Seq("Story") ++ (1 to topCommenter).map(idx => s"#${idx} Top Commenter")

  def run(): Seq[OverallCommentReport] = {
    val storiesID = service.fetchTopStories().get.ids.take(topStories) // FIXME better handling option
    val ctrl = new StoryCrawler()

    // good opportunity to parallelize here
    // val stories = storiesID.flatMap { storyID => ctrl.fetchStory(storyID) }

    val storiesF: Seq[Future[Option[Story]]] = storiesID.map { storyID =>
      Future {
        println(s".. loading $storyID @ ${Thread.currentThread().getName}")
        val start = System.currentTimeMillis()
        val story = ctrl.fetchStory(storyID)
        println(s".. loaded $storyID @ ${Thread.currentThread().getName}, took ${System.currentTimeMillis() - start} ms")
        story
      }
    }

    val storiesMergeF = Future.foldLeft(storiesF)(Seq.empty[Story])(_ ++ _)
    val stories = Await.result(storiesMergeF, timeout)

    Report.reportAll(stories, topCommenter)
  }

}
