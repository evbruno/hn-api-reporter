package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import br.etc.bruno.hn.model.ID
import br.etc.bruno.hn.services.HackerNewsAPI.Service

/**
 * Actor to load the top stories and its kids.
 */
object TopStoriesActor {

  /**
   * Maps a "Story ID" to a sequence of its children ("kids")
   */
  type StoryResponse = Set[ID]

  // Response commands
  final case class TopStoriesLoadedResponse(response: StoryResponse)

  // Request commands
  sealed trait TopStoriesCommand

  final case class Start(replyTo: ActorRef[TopStoriesLoadedResponse]) extends TopStoriesCommand

  def apply(topStories: Int)(implicit api: Service): Behavior[TopStoriesCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case Start(replyTo) =>
          // FIXME: async ?
          val storiesID = api.fetchTopStories().get.ids.take(topStories)
          replyTo ! TopStoriesLoadedResponse(storiesID.toSet)
          Behaviors.same
      }
    }
}
