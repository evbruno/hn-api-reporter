package br.etc.bruno.hn.app

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior, PostStop }
import br.etc.bruno.hn.HackerNewsAPI
import br.etc.bruno.hn.v2.StoryActor
import br.etc.bruno.hn.v2.StoryActor._
import br.etc.bruno.hn.api._

object AkkaMainApp extends App {

  // FIXME I'm using this delegate for now to control the response I want to see
  // remove it before submitting!!
  val delegateService = HackerNewsAPI.impl
  implicit val svc = new HackerNewsAPI.Service {

    override def fetchTopStories(): Option[TopStoriesResponse] =
      Some(TopStoriesResponse(Seq(27880018L, 27890992L, 27887434L)))

    override def fetchItem(id: Long): Option[ItemResponse] =
      delegateService.fetchItem(id)

  }

  val root: Behavior[NotUsed] = Behaviors.setup { context =>
    val crawler = context.spawn(StoryActor(), "StoryActor")
    val log = context.log

    val responder = context.spawn(Behaviors.receiveMessage[StoryLoaded] {
      case StoryLoaded(comments) =>
        log.info("I got the Comments: \n\t{}", comments.mkString("\n\t"))

        context.system.terminate()
        Behaviors.stopped

    }.receiveSignal {
      case (_, signal) if signal == PostStop =>
        println("Killing root! bye!")
        Behaviors.same
    }, "responder")

    val root = Seq(27898719L, 27895824L)
    crawler ! InitializeStory(root, responder)

    Behaviors.empty
  }

  ActorSystem(root, "root-actor")

}
