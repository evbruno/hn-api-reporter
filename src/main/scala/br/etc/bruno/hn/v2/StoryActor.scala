package br.etc.bruno.hn.v2

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, Routers }
import akka.actor.typed.{ ActorRef, Behavior, PostStop, SupervisorStrategy }
import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.api.ItemResponse
import br.etc.bruno.hn.model.{ Comment, ID }
import scala.util.Random

object CommentsActorWorker {

  import StoryActor._

  def apply()(implicit api: Service): Behavior[StoryCommand] = Behaviors.setup { context =>
    context.log.info(s"Starting worker at ${Thread.currentThread().getName}")

    Behaviors.receiveMessage[StoryCommand] {
      case Process(id, replyTo) =>
        context.log.info("Worker processing kid {}", id)

        // FIXME improve optional handling
        val item: ItemResponse = api.fetchItem(id).get

        val comment: Option[Comment] = for {
          parent <- item.parent
          author <- item.by
          kids <- item.kids.orElse(Some(Seq.empty))
          if !item.deleted.getOrElse(false)
        } yield
          Comment(item.id, parent, author, kids)

        replyTo ! WorkerResult(comment)

        Behaviors.same
    }.receiveSignal {
      case (_, signal) if signal == PostStop =>
        println("Killing worker! bye!")
        Behaviors.same
    }
  }

}

object StoryActor {

  case class StoryLoaded(result: Set[Comment])

  sealed trait StoryCommand

  case class Start(replyTo: ActorRef[StoryLoaded]) extends StoryCommand
  case object Tick extends StoryCommand
  case class Process(itemID: ID, replyTo: ActorRef[StoryCommand]) extends StoryCommand
  case class WorkerResult(result: Option[Comment]) extends StoryCommand

  // FSM event becomes the type of the message Actor supports

  def apply(storyId: ID, kids: Seq[ID])(implicit api: Service): Behavior[StoryCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case Start(replyTo) =>
          context.log.info("Loading {} kids for story id {}", kids.size, storyId)
          val wrk = buildCommentsWorker(context)
          val run = running(wrk, replyTo, kids, Seq.empty, 0)
          context.self ! Tick
          run
    }
  }

  private def running(comments: ActorRef[StoryCommand],
                      replyTo: ActorRef[StoryLoaded],
                      queue: Seq[ID],
                      accumulated: Seq[Comment],
                      pending: Int = 0): Behavior[StoryCommand] =
    Behaviors.receive[StoryCommand] { (context, message) =>
      message match {

        case Tick =>
          context.log.info(s"Tick, queue=${queue}")

          if (queue.isEmpty)
            Behaviors.same
          else {
            queue foreach { target =>
              comments ! Process(target, context.self)
            }
            // reset the Queue and continue...
            running(comments, replyTo, Seq.empty, accumulated, pending + queue.size)
          }

        case WorkerResult(maybeComment) =>
          context.log.info(s"Got result {}", maybeComment)

          val updatedResult = accumulated ++ maybeComment
          val updatedQueue = queue ++ maybeComment.map(_.kids).getOrElse(Seq.empty)

          if (updatedQueue.nonEmpty) {
            context.log.info(s"Non Empty queue, awaiting for {} kids, w/ {} pending execs", updatedQueue.size, pending)
            context.self ! Tick
            running(comments, replyTo, updatedQueue, updatedResult, pending - 1)
          } else if (pending >= 2) {
            context.log.info(s"Empty queue, still awaiting for {} execs", pending)
            running(comments, replyTo, updatedQueue, updatedResult, pending - 1)
          } else {
            context.log.info(s"Empty queue, all done!")
            replyTo ! StoryLoaded(updatedResult.toSet)
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
      Behaviors.supervise(CommentsActorWorker())
        .onFailure[Exception](SupervisorStrategy.restart)
    }

    context.spawn(pool, "comments-worker-pool")
  }
}