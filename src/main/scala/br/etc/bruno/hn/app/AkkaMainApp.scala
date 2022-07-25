package br.etc.bruno.hn.app

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior, PostStop }
import br.etc.bruno.hn.actors.ApplicationActor
import br.etc.bruno.hn.actors.ApplicationActor.{ AppResponse, StoriesLoaded }
import br.etc.bruno.hn.api._
import br.etc.bruno.hn.model.Story
import br.etc.bruno.hn.services.{ HackerNewsAPI, Report, ReportPrinter }

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

  private def newRoot(topStories: Int, topCommenter: Int): Behavior[NotUsed] =
    Behaviors.setup[NotUsed] { context =>
      val applicationActor = context.spawn(ApplicationActor(topStories), "app-actor")
      val log = context.log

      val rootHandler = context.spawn(Behaviors.receiveMessage[AppResponse] {

        case StoriesLoaded(response) =>
          log.info("Root handler got the response size: {}", response.size)
          report(response.toSet, topCommenter)
          context.system.terminate()
          Behaviors.stopped

        case _ =>
          Behaviors.unhandled

      }.receiveSignal {
        case (_, signal) if signal == PostStop =>
          log.info("Terminating... maybe free some resources !?")
          Behaviors.same
      },
      "root-handler")

      applicationActor ! ApplicationActor.Start(rootHandler)

      Behaviors.empty
    }

  def report(reports: Set[(Story, Set[Report.CommentReport])], topCommenter: Int): Unit = {
    val rows = Report.aggregate(reports, topCommenter)
    val data = new ReportPrinter(rows, topCommenter, div = "|").tabularData()
    println(data)
  }

}
