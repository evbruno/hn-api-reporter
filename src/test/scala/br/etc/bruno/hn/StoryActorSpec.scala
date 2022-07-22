package br.etc.bruno.hn

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import br.etc.bruno.hn.model.Comment
import br.etc.bruno.hn.v2.StoryActor
import br.etc.bruno.hn.v2.StoryActor.StoryLoaded
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._

class StoryActorSpec
  extends AnyFreeSpec
    with BeforeAndAfterAll
    //    with LogCapturing
    with Matchers
    with MockAPI {

  implicit val svc = mockService

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "story actor should reply with 4 comments" in {
    val subject = testKit.spawn(StoryActor())
    val probe = testKit.createTestProbe[StoryActor.StoryLoaded]()

    subject ! StoryActor.InitializeStory(Kids(TopStories(0)), probe.ref)

    probe.expectMessage(StoryLoaded(Set(
      Comment(27895824, 27880018, "mftb", List(27896396, 27898570)),
      Comment(27898719, 27880018, "adultSwim", List()),
      Comment(27898570, 27895824, "mftb", List()),
      Comment(27896396, 27895824, "rambambram", List(27900391))
    )))

    probe.expectNoMessage(50.millis)
  }


}
