package br.etc.bruno.hn.app

import akka.NotUsed
import akka.actor.typed.{ ActorSystem, Behavior, PostStop }
import akka.actor.typed.scaladsl.Behaviors
import br.etc.bruno.hn.{ HackerNewsAPI, Report }
import br.etc.bruno.hn.actors.ApplicationActor
import br.etc.bruno.hn.actors.ApplicationActor.{ AppResponse, StoriesLoaded }
import br.etc.bruno.hn.api._
import br.etc.bruno.hn.model.Story

object AkkaMainApp {

  // FIXME I'm using this delegate for now to control the response I want to see
  // remove it before submitting!!
  val delegateService = HackerNewsAPI.impl
  implicit val svc = new HackerNewsAPI.Service {

    override def fetchTopStories(): Option[TopStoriesResponse] =
      Some(TopStoriesResponse(Seq(27880018L, 27890992L, 27887434L)))

    override def fetchItem(id: Long): Option[ItemResponse] =
      delegateService.fetchItem(id)

  }

  def main(args: Array[String]): Unit = {
    // TODO handle better arguments
    val (topStories, topCommenter) =
      if (args.length == 2) (args(0).toInt, args(1).toInt)
      else (30, 5)

    val app = newRoot(topStories, topCommenter)

    ActorSystem(app, "root-actor")
  }


  def newRoot(topStories: Int, topCommenter: Int): Behavior[NotUsed] =
    Behaviors.setup[NotUsed] { context =>
      val applicationActor = context.spawn(ApplicationActor(topCommenter, topStories), "StoryActor")
      val log = context.log

      val rootHandler = context.spawn(Behaviors.receiveMessage[AppResponse] {
        case StoriesLoaded(response) =>
          log.debug("Root handler got the response: {}", response.mkString("\n\t", "\n\t", ""))

          report(response, topCommenter)

          context.system.terminate()
          Behaviors.stopped

      }.receiveSignal {
        case (_, signal) if signal == PostStop =>
          log.info("Terminating... maybe free some resources !?")
          Behaviors.same
      }, "root-handler")

      applicationActor ! ApplicationActor.Start(rootHandler)

      Behaviors.empty
    }

  //TODO using plain `println` here
  //TODO make a nice tabular grid?
  def report(stories: Set[Story], topCommenter: Int) = {
    val div = " | "

    val header =( Seq("Story") ++
      (1 to topCommenter).map(idx => s"#${idx} Top Commenter")
    ).mkString(div)

    val reportedValues = Report
      .reportAll(stories.toSeq.sortBy(_.id), topCommenter)
      .map(_.prettyColumns.mkString(div))
      .mkString("\n")

    println(header)
    println(reportedValues)
  }

}
