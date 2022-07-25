package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, Routers }
import akka.actor.typed.{ ActorRef, ActorTags, Behavior, SupervisorStrategy }
import br.etc.bruno.hn.model.{ Comment, ID, Story }
import br.etc.bruno.hn.services.HackerNewsAPI.Service
import br.etc.bruno.hn.services.Report.CommentReport

/**
 * Actor to load and reduce the Comments.
 *
 * Use this one instead of [[StoryActor]]
 */
object StoryReducerActor {

  // Response commands
  sealed trait StoryResponse
  final case class StoryReduced(story: Story, report: Set[CommentReport]) extends StoryResponse

  // Request commands
  sealed trait StoryCommand
  final case class Start(replyTo: ActorRef[StoryReduced]) extends StoryCommand
  final case class Process(itemID: ID, replyTo: ActorRef[StoryCommand]) extends StoryCommand
  final case class WorkerResult(result: Option[Comment]) extends StoryCommand

  // Internal protocol
  private final case object Dequeue extends StoryCommand

  /**
   * Uses the blocking api [[Service]] to load the item details
   *
   * next state: [[initializing()]]
   *
   * @param storyId
   * @param api
   * @return
   */
  def apply(storyId: ID)(implicit api: Service): Behavior[StoryCommand] = {
    api.fetchItem(storyId) match {
      case None               =>
        Behaviors.empty
      case Some(itemResponse) =>
        val title = itemResponse.title.getOrElse("<no title>")
        val subject = Story(storyId, title, Map.empty)
        initializing(subject, itemResponse.kids.getOrElse(Seq.empty))
    }
  }

  /**
   * Waits for the caller to start listening.
   * This is the step where the Router is build, and each Story will use up to 4
   * "workers"
   *
   * next state: [[running()]]
   *
   * @see [[buildCommentsWorker()]]
   * @param story
   * @param kids
   * @param api
   * @return
   */
  private def initializing(story: Story,
                           kids: Seq[ID])
                          (implicit api: Service): Behavior[StoryCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case Start(replyTo) =>
          context.log.debug("Initializing {} kids for story ({}) {}", kids.size, story.id, story.title)
          if (kids.isEmpty) {
            replyTo ! StoryReduced(story, Set.empty)
            Behaviors.empty
          } else {
            context.self ! Dequeue
            running(story, buildCommentsWorker(context), replyTo, kids)
          }
        case _              =>
          Behaviors.unhandled
      }
    }

  /**
   * Aggregates all the messages from their [[CommentsActor]] workers.
   *
   * This is the final state, and will reply with the preliminary reports to its
   * caller.
   *
   * @param story
   * @param commentActor
   * @param replyTo
   * @param queue
   * @param accumulated
   * @param pending
   * @return
   */
  private def running(story: Story,
                      commentActor: ActorRef[StoryCommand],
                      replyTo: ActorRef[StoryReduced],
                      queue: Seq[ID] = Seq.empty,
                      accumulated: Map[String, CommentReport] = Map.empty,
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
          context.log.debug(s"Got result {}", maybeComment)

          val updatedQueue = queue ++ maybeComment.map(_.kids).getOrElse(Seq.empty)
          val updatedResult = maybeComment.fold(accumulated) { comment =>
            accumulated.updatedWith(comment.user)  {
              case Some(rep) =>
                Some(rep + 1)
              case None =>
                Some(CommentReport(comment.user, 1))
            }
          }

          if (updatedQueue.nonEmpty) {
            context.log.debug(s"Non Empty queue, awaiting for {} kids, w/ {} pending execs", updatedQueue.size, pending)
            context.self ! Dequeue
            running(story, commentActor, replyTo, updatedQueue, updatedResult, pending - 1)
          } else if (pending >= 2) {
            context.log.debug(s"Empty queue, still awaiting for {} execs", pending)
            running(story, commentActor, replyTo, updatedQueue, updatedResult, pending - 1)
          } else {
            context.log.debug(s"Empty queue, all done for story {}", story)
            replyTo ! StoryReduced(story, updatedResult.values.toSet)
            Behaviors.empty
          }

        case _ =>
          Behaviors.unhandled
      }

    }

  private def buildCommentsWorker(context: ActorContext[StoryCommand])
                                 (implicit api: Service): ActorRef[StoryCommand] = {
    val pool = Routers.pool(poolSize = 4) {
      Behaviors.supervise(CommentsActor())
        .onFailure[Exception](SupervisorStrategy.restart)
    }

    context.spawn(pool, "comments-worker-pool", ActorTags("comments-worker-pool"))
  }
}
