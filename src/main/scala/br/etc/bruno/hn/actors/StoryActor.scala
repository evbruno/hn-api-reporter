package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, Routers }
import akka.actor.typed.{ ActorRef, Behavior, PostStop, SupervisorStrategy }
import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.api.ItemResponse
import br.etc.bruno.hn.model.{ Comment, ID, Story }
import br.etc.bruno.hn.actors.StoryActor.{ StoryCommand, StoryLoaded }
import scala.util.Random

/**
 * Actor to
 */
object StoryActor {

  // Response commands
  sealed trait StoryResponse
  final case class StoryLoaded(story: Story) extends StoryResponse

  // Request commands
  sealed trait StoryCommand
  final case class Start(replyTo: ActorRef[StoryLoaded]) extends StoryCommand
  final case class Process(itemID: ID, replyTo: ActorRef[StoryCommand]) extends StoryCommand
  final case class WorkerResult(result: Option[Comment]) extends StoryCommand

  // Internal protocol
  private final case object Dequeue extends StoryCommand

  def apply(storyId: ID)(implicit api: Service): Behavior[StoryCommand] = {
    api.fetchItem(storyId) match {
      case None               =>
        Behaviors.empty
      case Some(itemResponse) =>
        val kids: Seq[ID] = itemResponse.kids.getOrElse(Seq.empty)
        val title = itemResponse.title.getOrElse("<no title>")
        val subject = Story(storyId, title, Map.empty)
//        initializing(storyId, itemResponse.kids.getOrElse(Seq.empty))
        initializing(subject, itemResponse.kids.getOrElse(Seq.empty))
    }
  }

  private def initializing(story: Story,
                           kids: Seq[ID])
                          (implicit api: Service): Behavior[StoryCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case Start(replyTo) =>
          context.log.info("Initializing {} kids for story ({}) {}", kids.size, story.id, story.title)
          context.self ! Dequeue
          running(story, buildCommentsWorker(context), replyTo, kids)
      }
    }

  private def running(story: Story,
                      commentActor: ActorRef[StoryCommand],
                      replyTo: ActorRef[StoryLoaded],
                      queue: Seq[ID] = Seq.empty,
                      accumulated: Map[ID, Comment] = Map.empty,
                      pending: Int = 0): Behavior[StoryCommand] =
    Behaviors.receive[StoryCommand] { (context, message) =>
      message match {

        case Dequeue =>
          if (queue.isEmpty)
            Behaviors.same
          else {
            queue foreach { target =>
              commentActor ! Process(target, context.self)
            }

            // reset the "Queue" and continue...
            running(story, commentActor, replyTo, Seq.empty, accumulated, pending + queue.size)
          }

        case WorkerResult(maybeComment) =>
          context.log.info(s"Got result {}", maybeComment)

          val updatedQueue = queue ++ maybeComment.map(_.kids).getOrElse(Seq.empty)
          val updatedResult = maybeComment.fold(accumulated) { c => accumulated + (c.id -> c)}

          if (updatedQueue.nonEmpty) {
            context.log.info(s"Non Empty queue, awaiting for {} kids, w/ {} pending execs", updatedQueue.size, pending)
            context.self ! Dequeue
            running(story, commentActor, replyTo, updatedQueue, updatedResult, pending - 1)
          } else if (pending >= 2) {
            context.log.info(s"Empty queue, still awaiting for {} execs", pending)
            running(story, commentActor, replyTo, updatedQueue, updatedResult, pending - 1)
          } else {
            context.log.info(s"Empty queue, all done!")
            val result = story.copy(kids = updatedResult)
            replyTo ! StoryLoaded(result)
            Behaviors.empty
          }

        case _ =>
          context.log.error("unhandled...")
          Behaviors.unhandled
      }

    }.receiveSignal {
      case (_, signal) if signal == PostStop =>
        println("Killing story! bye!")
        Behaviors.same
    }


  private def buildCommentsWorker(context: ActorContext[StoryCommand])(implicit api: Service) = {
    val pool = Routers.pool(poolSize = 4) {
      Behaviors.supervise(CommentsActor())
        .onFailure[Exception](SupervisorStrategy.restart)
    }

    context.spawn(pool, "comments-worker-pool")
  }
}
