package br.etc.bruno.hn

import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.Report.OverallCommentReport

class Application(topCommenter: Int, topStories: Int)(implicit val service: Service) {

  def header(): Seq[String] =
    Seq("Story") ++ (1 to topCommenter).map(idx =>  s"#${idx} Top Commenter")

  def run(): Seq[OverallCommentReport] = {
    val storiesID = service.fetchTopStories().get.ids.take(topStories) // FIXME better handling option
    val ctrl = new StoryCrawler()

    val stories = storiesID.flatMap { storyID => ctrl.fetchStory(storyID) }

    Report.reportAll(stories, topCommenter)
  }

}
