package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import br.etc.bruno.hn.model.ID
import br.etc.bruno.hn.services.HackerNewsAPI.Service

/**
 * Actor to load the top stories and its kids.
 */
object TopStoriesActor {

  type StoryResponse = Set[ID]

  // Response commands
  final case class TopStoriesLoadedResponse(response: StoryResponse)

  // Request commands
  sealed trait TopStoriesCommand

  final case class Start(replyTo: ActorRef[TopStoriesLoadedResponse]) extends TopStoriesCommand

  def apply(topStories: Int)(implicit api: Service): Behavior[TopStoriesCommand] =
    Behaviors.receiveMessage {
      case Start(replyTo) =>
        val storiesID = api.fetchTopStories().map(_.ids.toSet.take(topStories)).getOrElse(Set.empty)
        replyTo ! TopStoriesLoadedResponse(storiesID)
        Behaviors.same
      case _              =>
        Behaviors.unhandled
    }

}
