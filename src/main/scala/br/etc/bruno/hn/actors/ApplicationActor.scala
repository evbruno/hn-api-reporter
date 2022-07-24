package br.etc.bruno.hn.actors

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import br.etc.bruno.hn.HackerNewsAPI.Service
import br.etc.bruno.hn.actors.StoryActor.StoryCommand
import br.etc.bruno.hn.model._

object ApplicationActor {

  // reply command(s)
  sealed trait AppResponse

  final case class StoriesLoaded(result: Set[Story]) extends AppResponse

  case object NoStoriesFound extends AppResponse

  // request commands
  sealed trait AppCommand

  final case class AStart(replyTo: ActorRef[AppResponse]) extends AppCommand

  def apply(topCommenter: Int = 2,
            topStories: Int = 3)
           (implicit svc: Service): Behavior[AppCommand] = Behaviors.setup { context =>

    svc.fetchTopStories() match {
      case None =>
        Behaviors.empty
      case Some(stories) =>
        initializing(stories.ids.take(topStories), context, topCommenter)
    }
  }

    private def initializing(stories: Seq[ID],
                            context: ActorContext[AppCommand],
                            topCommenter: Int = 2)
                           (implicit svc: Service): Behavior[AppCommand] =  {

      // spawn 1 child actor per story
      val pending = stories.size
      val storiesActor: Seq[ActorRef[StoryCommand]] = stories.map { id =>
        context.spawn(StoryActor(id), s"story-actor-${id}")
      }


//      val aggregator = context.spawn(Behaviors.receiveMessage[StoryLoaded] {
//  //      case Stor=>
//        Behaviors.same
//      })
  //
  //    val children: Seq[ActorRef[_]] = stories.map { id =>
  //      val apiResponse = svc.fetchItem(id)
  //      if (apiResponse.isEmpty)
  //        None
  //      else
  //        context.spawn(StoryActor(id, apiResponse.get.kids.get), s"story-actor-${id}")
  //    }
  //
  //    ???
  //
      Behaviors.same
    }


//    Behaviors.receive { (message, context) =>
//      message match {
//        case AStart(replyTo) =>
//          // FIXME async service?
//          svc fetchTopStories() match {
//            case None          =>
//              replyTo ! NoStoriesFound
//              Behaviors.empty
//            case Some(stories) =>
//              Behaviors.empty
//            //              initializing(stories.ids.take(topStories), replyTo, context, topCommenter)
//          }
//      }
//    }

  //  private def initializing(stories: Seq[ID],
  //                          replyTo: ActorRef[AppResponse],
  //                          context: ActorContext[AppCommand],
  //                          topCommenter: Int = 2)
  //                         (implicit svc: Service): Behavior[AppCommand] = {
  //
  //    // spawn 1 child actor per story
  //    val pending = stories.size
  //    val storiesActor: Seq[ActorRef[StoryCommand]] = stories.map { id =>
  //      context.spawn(StoryActor(id), s"story-actor-${id}")
  //    }
  //
  ////    val aggregator = context.spawn(Behaviors.receiveMessage[StoryLoaded] {
  //////      case Stor=>
  ////      Behaviors.same
  ////    })
  ////
  ////    val children: Seq[ActorRef[_]] = stories.map { id =>
  ////      val apiResponse = svc.fetchItem(id)
  ////      if (apiResponse.isEmpty)
  ////        None
  ////      else
  ////        context.spawn(StoryActor(id, apiResponse.get.kids.get), s"story-actor-${id}")
  ////    }
  ////
  ////    ???
  ////
  //    Behaviors.same
  //  }
}
