package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.util.Timeout
import br.etc.bruno.hn.services.HackerNewsAPI.Service
import br.etc.bruno.hn.actors.StoryReducerActor.StoryResponse
import br.etc.bruno.hn.model._
import br.etc.bruno.hn.services.Report.CommentReport
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure, Success }

object ApplicationActor {

  // Response commands
  sealed trait AppResponse
  final case class StoriesLoaded(result: Map[Story, Set[CommentReport]]) extends AppResponse

  // old version
  //final case class StoriesLoaded(result: Set[Story]) extends AppResponse

  // Request commands
  sealed trait AppCommand
  final case class Start(replyTo: ActorRef[AppResponse]) extends AppCommand

  // Internal protocol
  private case class TopStoriesLoadedWrapped(res: TopStoriesActor.StoryResponse) extends AppCommand
  private case class AllStoriesLoadedWrapped(result: Map[Story, Set[CommentReport]]) extends AppCommand

  // old version
  //private case class AllStoriesLoadedWrapped(result: Set[Story]) extends AppCommand

  def apply(topStories: Int = 3)
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

        initializing(context, buffer)
      }
    }
  }

  private def initializing(context: ActorContext[AppCommand],
                           buffer: StashBuffer[AppCommand])
                          (implicit api: Service): Behaviors.Receive[AppCommand] =
    Behaviors.receiveMessage[AppCommand] {
      case TopStoriesLoadedWrapped(stories) =>
        buffer.unstashAll(loadStories(stories, context))
      case other                             =>
        // stash all other messages for later processing
        buffer.stash(other)
        Behaviors.same

    }

  private def loadStories(stories: TopStoriesActor.StoryResponse,
                            context: ActorContext[AppCommand])
                           (implicit api: Service): Behavior[AppCommand] =
    Behaviors.receiveMessage {
      case Start(replyTo) =>
        context.log.info("Loaded {} top stories", stories.size)
        accumulatingStories(stories, replyTo)
      case _ =>
        Behaviors.unhandled
    }

  private def accumulatingStories(
                                 stories: TopStoriesActor.StoryResponse,
                                 replyTo: ActorRef[AppResponse])
                               (implicit api: Service): Behavior[AppCommand] =
    Behaviors.setup { context =>

      val accumulator = context.spawnAnonymous(newAccumulator(context, stories.size))

      stories foreach { storyId =>
          val act = context.spawnAnonymous(StoryReducerActor(storyId))
          act ! StoryReducerActor.Start(accumulator)
      }

      Behaviors.receiveMessage {
        case AllStoriesLoadedWrapped(response) =>
          replyTo ! StoriesLoaded(response)
          Behaviors.same
        case _                                 =>
          Behaviors.unhandled
      }
    }

  def newAccumulator(context: ActorContext[AppCommand],
                     pending: Int,
                     acc: Map[Story, Set[CommentReport]] = Map.empty): Behaviors.Receive[StoryResponse] =
    Behaviors.receiveMessage {
      case StoryReducerActor.StoryReduced(story, report) =>
        val updated = acc + (story -> report)

        if (pending > 1)
          newAccumulator(context, pending - 1, updated)
        else {
          context.self ! AllStoriesLoadedWrapped(updated)
          Behaviors.stopped
        }
    }

}
