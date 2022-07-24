package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.util.Timeout
import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.actors.StoryActor.StoryResponse
import br.etc.bruno.hn.model._
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

object ApplicationActor {

  // Response commands
  sealed trait AppResponse
  final case class StoriesLoaded(result: Set[Story]) extends AppResponse
  final case object NoStoriesFound extends AppResponse

  // Request commands
  sealed trait AppCommand
  final case class Start(replyTo: ActorRef[AppResponse]) extends AppCommand

  // Internal protocol
  private case class TopStoriesLoadedWrapped(res: TopStoriesActor.StoryResponse) extends AppCommand
  private case class AllStoriesLoadedWrapped(result: Set[Story]) extends AppCommand

  def apply(topCommenter: Int = 2,
            topStories: Int = 3)
           (implicit api: Service): Behavior[AppCommand] = {
    Behaviors.withStash(8) { buffer =>
      Behaviors.setup[AppCommand] { context =>
        implicit val timeout: Timeout = 10.minutes
        val topper = context.spawn(TopStoriesActor(topStories), "top-stories")

        context.ask(topper, replyTo => TopStoriesActor.Start(replyTo)) {
          case Success(value)     =>
            TopStoriesLoadedWrapped(value.response)
          case Failure(exception) =>
            exception.printStackTrace()
            context.log.error(exception.getMessage)
            TopStoriesLoadedWrapped(Set.empty)
        }

        initializing(context, buffer, topCommenter)
      }
    }
  }

  private def initializing(context: ActorContext[AppCommand],
                           buffer: StashBuffer[AppCommand],
                           topCommenter: Int = 2)
                          (implicit api: Service): Behaviors.Receive[AppCommand] =
    Behaviors.receiveMessage[AppCommand] {
      case TopStoriesLoadedWrapped(stories) =>
        buffer.unstashAll(loadStories(stories, context, topCommenter))
      case other                             =>
        // stash all other messages for later processing
        buffer.stash(other)
        Behaviors.same

    }

  private def loadStories(stories: TopStoriesActor.StoryResponse,
                            context: ActorContext[AppCommand],
                            topCommenter: Int = 2)
                           (implicit api: Service): Behavior[AppCommand] =
    Behaviors.receiveMessage {
      case Start(replyTo) =>
        context.log.info("Loaded {} top stories", stories.size)
        accumulatingStories(stories, replyTo, topCommenter)
    }

  private def accumulatingStories(
                                 stories: TopStoriesActor.StoryResponse,
                                 replyTo: ActorRef[AppResponse],
                                 topCommenter: Int = 2)
                               (implicit api: Service): Behavior[AppCommand] =
    Behaviors.setup { context =>

      val accumulator = context.spawnAnonymous(newAccumulator(context, stories.size))

      stories foreach { storyId =>
          val act = context.spawnAnonymous(StoryActor(storyId))
          act ! StoryActor.Start(accumulator)
      }

      Behaviors.receiveMessage {
        case AllStoriesLoadedWrapped(response) =>
          replyTo ! StoriesLoaded(response)
          Behaviors.same
        case t                                 =>
          context.log.info("what? {}", t)
          Behaviors.same
      }
    }

  def newAccumulator(context: ActorContext[AppCommand],
                     pending: Int,
                     acc: Set[Story] = Set.empty): Behaviors.Receive[StoryResponse] =
    Behaviors.receiveMessage {
      case StoryActor.StoryLoaded(story: Story) =>
        val updated: Set[Story] = acc + story

        if (pending > 1)
          newAccumulator(context, pending - 1, updated)
        else {
          context.self ! AllStoriesLoadedWrapped(updated)
          Behaviors.stopped
        }
    }

}
