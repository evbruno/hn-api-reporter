package br.etc.bruno.hn

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import br.etc.bruno.hn.actors.StoryActor.StoryLoaded
import br.etc.bruno.hn.actors.TopStoriesActor.TopStoriesLoadedResponse
import br.etc.bruno.hn.actors.{ ApplicationActor, StoryActor, StoryReducerActor, TopStoriesActor }
import br.etc.bruno.hn.model.{ Comment, Story }
import br.etc.bruno.hn.services.Report.CommentReport
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._

class StoryActorSpec
  extends AnyFreeSpec
    with BeforeAndAfterAll
    with Matchers
    with MockAPI {

  implicit val svc = mockService

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "top stories actor should reply top 2 stories" in {
    val subject = testKit.spawn(TopStoriesActor(2))
    val probe = testKit.createTestProbe[TopStoriesActor.TopStoriesLoadedResponse]()

    subject ! TopStoriesActor.Start(probe.ref)

    probe.expectMessage(TopStoriesLoadedResponse(Set(27888626L, 27880018L)))
  }

  "deprecated story actor should reply with 4 comments for the 1st Story" in {
    val storyId = TopStories(0)
    val subject = testKit.spawn(StoryActor(storyId))
    val probe = testKit.createTestProbe[StoryActor.StoryLoaded]()

    subject ! StoryActor.Start(probe.ref)

    probe.expectMessage(StoryLoaded(
      Story(
        id = 27880018,
        title = "Thoughts on \"The Theory and Craft of Digital Preservation\"",
        kids = Map(
          27895824L -> Comment(27895824, 27880018, "mftb", List(27896396, 27898570)),
          27898719L -> Comment(27898719, 27880018, "adultSwim", List()),
          27898570L -> Comment(27898570, 27895824, "mftb", List()),
          27896396L -> Comment(27896396, 27895824, "rambambram", List(27900391))
        )
      )
    ))

    probe.expectNoMessage(50.millis)
  }

  "story reducer actor should reply with 4 comments reports for the 2nd Story" in {
    val storyId = TopStories(1)
    val subject = testKit.spawn(StoryReducerActor(storyId))
    val probe = testKit.createTestProbe[StoryReducerActor.StoryReduced]()

    subject ! StoryReducerActor.Start(probe.ref)

    probe.expectMessage(
      StoryReducerActor.StoryReduced(
        Story(27888626L, "Excuse Me… Some Digital Preservation Fallacies? (2006)"),
        Set(
          CommentReport("Jiro", 1),
          CommentReport("kmeisthax", 2),
          CommentReport("TazeTSchnitzel", 1),
          CommentReport("EricE", 1)
        )
      )
    )

    probe.expectNoMessage(50.millis)
  }

  "application actor should reply with everything" in {
    val subject = testKit.spawn(ApplicationActor(10))
    val probe = testKit.createTestProbe[ApplicationActor.AppResponse]()

    subject ! ApplicationActor.Start(probe.ref)

    probe.expectMessage(ApplicationActor.StoriesLoaded(Map(
      Story(27888626L, "Excuse Me… Some Digital Preservation Fallacies? (2006)") ->
        Set(
          CommentReport("Jiro", 1),
          CommentReport("kmeisthax", 2),
          CommentReport("TazeTSchnitzel", 1),
          CommentReport("EricE", 1)
        ),
      Story(27880018L, "Thoughts on \"The Theory and Craft of Digital Preservation\"") ->
        Set(
          CommentReport("adultSwim", 1),
          CommentReport("mftb", 2),
          CommentReport("rambambram", 1),
        ),
    )))

    probe.expectNoMessage(50.millis)
  }


}
