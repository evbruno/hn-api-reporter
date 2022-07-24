package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.model.ID
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

object TopStoriesActor {

  // Responses
  /**
   * Maps a "Story ID" to a sequence of its children ("kids")
   */
  type StoryResponse = Map[ID, Seq[ID]]
  final case class TopStoriesLoadedResponse(r: StoryResponse)

  // Request commands
  sealed trait TopStoriesCommand
  final case class Start(replyTo: ActorRef[TopStoriesLoadedResponse]) extends TopStoriesCommand

  // Internals
  private final case class TopStoriesLoaded(r: StoryResponse) extends TopStoriesCommand

  def apply(topStories: Int)(implicit api: Service): Behavior[TopStoriesCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case Start(replyTo) =>
          implicit val ec = context.executionContext

          // FIXME
          val storiesID = api.fetchTopStories().get.ids.take(topStories)

          context.pipeToSelf(loadTopStores(storiesID, context)) {
            case Success(value) =>
              TopStoriesLoaded(value)
            case Failure(t)     =>
              context.log.error(t.getMessage)
              TopStoriesLoaded(Map.empty)
          }

          loadingTopStories(replyTo)
      }
    }

  private def loadingTopStories(replyTo: ActorRef[TopStoriesLoadedResponse]): Behavior[TopStoriesCommand] =
    Behaviors.receiveMessage {
      case TopStoriesLoaded(stories) =>
        replyTo ! TopStoriesLoadedResponse(stories)
        Behaviors.same
    }

  private def loadTopStores(storiesID: Seq[ID],
                            context: ActorContext[TopStoriesCommand])
                           (implicit api: Service,
                            ec: ExecutionContext): Future[StoryResponse] = {

    val storiesF = storiesID.map { storyID =>
      Future {
        val story = api.fetchItem(storyID)
        storyID -> story.get.kids.getOrElse(Seq.empty)
      }
    }

    Future.foldLeft(storiesF)(Map[ID, Seq[ID]]())(_ + _)
  }
}
