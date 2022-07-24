package br.etc.bruno.hn

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import br.etc.bruno.hn.actors.ApplicationActor.StoriesLoaded
import br.etc.bruno.hn.model.{ Comment, Story }
import br.etc.bruno.hn.actors.{ ApplicationActor, StoryActor, TopStoriesActor }
import br.etc.bruno.hn.actors.StoryActor.StoryLoaded
import br.etc.bruno.hn.actors.TopStoriesActor.TopStoriesLoadedResponse
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

    //    probe.expectMessage(TopStoriesLoadedResponse(Map(
    //      27888626L -> List(27890299L, 27889132L),
    //      27880018L -> List(27898719L, 27895824L),
    //    )))
    probe.expectMessage(TopStoriesLoadedResponse(Set(
      27888626L,
      27880018L
    )))
  }

  "story actor should reply with 4 comments for the 1st Story" in {
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

  "application actor should reply with everything" in {
    val subject = testKit.spawn(ApplicationActor(10, 10))
    val probe = testKit.createTestProbe[ApplicationActor.AppResponse]()

    subject ! ApplicationActor.Start(probe.ref)

    probe.expectMessage(StoriesLoaded(Set(
      Story(
        id = 27880018,
        title = "Thoughts on \"The Theory and Craft of Digital Preservation\"",
        kids = Map(
          27895824L -> Comment(27895824, 27880018, "mftb", List(27896396, 27898570)),
          27898719L -> Comment(27898719, 27880018, "adultSwim", List()),
          27898570L -> Comment(27898570, 27895824, "mftb", List()),
          27896396L -> Comment(27896396, 27895824, "rambambram", List(27900391))
        )
      ),
      Story(
        id = 27888626L,
        title = "Excuse Me… Some Digital Preservation Fallacies? (2006)",
        kids = Map(
          27889132L -> Comment(27889132, 27888626, "Jiro", List()),
          27899552L -> Comment(27899552, 27890724, "kmeisthax", List(27919417)),
          27890724L -> Comment(27890724, 27890299, "TazeTSchnitzel", List(27899552)),
          27890299L -> Comment(27890299, 27888626, "kmeisthax", List(27890724)),
          27919417L -> Comment(27919417, 27899552, "EricE", List())
        )
      )
    )))

    probe.expectNoMessage(50.millis)
  }


}
