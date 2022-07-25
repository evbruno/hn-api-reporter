package br.etc.bruno.hn.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import br.etc.bruno.hn.services.HackerNewsAPI.Service
import br.etc.bruno.hn.model.Comment

/**
 * Worker Actor that listens to [[Process]] messages to process specific
 * [[Comment]]'s
 *
 * @see [[StoryActor]]
 */
object CommentsActor {

  import StoryReducerActor._

  def apply()(implicit api: Service): Behavior[StoryCommand] =
    Behaviors.setup { context =>

      context.log.info(s"Starting worker at {}", Thread.currentThread().getName)

      Behaviors.receiveMessage[StoryCommand] {
        case Process(id, replyTo) =>
          context.log.debug("Worker processing kid {}", id)

          val comment = api.fetchItem(id).flatMap { item =>
            for {
              parent <- item.parent
              author <- item.by
              kids <- item.kids.orElse(Some(Seq.empty))
              if !item.deleted.getOrElse(false)
            } yield
              Comment(item.id, parent, author, kids)
          }

          replyTo ! WorkerResult(comment)

          Behaviors.same

        case _ =>
          Behaviors.unhandled
      }
    }

}
