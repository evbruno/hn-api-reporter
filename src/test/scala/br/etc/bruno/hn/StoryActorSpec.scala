package br.etc.bruno.hn

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import br.etc.bruno.hn.model.Comment
import br.etc.bruno.hn.actors.{ StoryActor, TopStoriesActor }
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

    probe.expectMessage(TopStoriesLoadedResponse(Map(
      27888626L -> List(27890299L, 27889132L),
      27880018L -> List(27898719L, 27895824L),
    )))
  }

  "story actor should reply with 4 comments" in {
    val storyId = TopStories(0)
    val subject = testKit.spawn(StoryActor(storyId))
    val probe = testKit.createTestProbe[StoryActor.StoryLoaded]()

    subject ! StoryActor.Start(probe.ref)

    probe.expectMessage(StoryLoaded(Set(
      Comment(27895824, 27880018, "mftb", List(27896396, 27898570)),
      Comment(27898719, 27880018, "adultSwim", List()),
      Comment(27898570, 27895824, "mftb", List()),
      Comment(27896396, 27895824, "rambambram", List(27900391))
    )))

    probe.expectNoMessage(50.millis)
  }


}
