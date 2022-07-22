package br.etc.bruno.hn.v2

import akka.NotUsed
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, Routers }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, SupervisorStrategy }
import br.etc.bruno.hn.v2.TreeWalkerActor.{ Initialize, TreeWalked }
import br.etc.bruno.hn.api.ItemResponse
import scala.util.Random

object api {

  val Root = Seq(1, 2, 3)
  val Tree = Map[Int, Seq[Int]](
    1 -> Seq(11, 12),
    2 -> Seq(21, 22),
    3 -> Seq(31, 32),
    12 -> Seq(120),
    22 -> Seq(220, 221),
    31 -> Seq(310),
  )
}

object Worker {

  import TreeWalkerActor._

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info(s"Starting worker at ${Thread.currentThread().getName}")

    Behaviors.receiveMessage {
      case Process(kid, replyTo) =>
        val rn = new Random().nextLong(1500)

        context.log.info("Worker processing kid {}, sleeping for {} ms", kid, rn)
        Thread.sleep(rn)

        val result = api.Tree.get(kid).getOrElse(Seq.empty)
        replyTo ! Result(kid, result)

        Behaviors.same
    }
  }
}

object TreeWalkerActor {

  type Kid = Int
  type Kids = Seq[Kid]

  case class TreeWalked(ids: Kids)

  trait Command
  case class Initialize(kids: Kids, replyTo: ActorRef[TreeWalked]) extends Command
  case object Tick extends Command
  case class Process(kid: Kid, replyTo: ActorRef[Command]) extends Command
  case class Result(kid: Int, result: Kids) extends Command

  def apply(): Behavior[Command] = Behaviors.receive { (context, message) =>
    message match {

      case Initialize(numbers, replyTo) =>
        context.log.info("starting new Walker...")
        val router = buildWorkers(context)
        val newBehavior = apply(router, replyTo, numbers, Seq.empty)
        context.self ! Tick // FIXME I'm not sure about this
        newBehavior

      case _                   =>
        context.log.error("unhandled...")
        Behaviors.unhandled
    }
  }

  private def buildWorkers(context: ActorContext[Command]) = {
    val pool = Routers.pool(poolSize = 2) {
      Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.restart)
    }
    context.spawn(pool, "worker-pool")
  }

  private def apply(workers: ActorRef[Command],
                    replyTo: ActorRef[TreeWalked],
                    queue: Seq[Int],
                    results: Seq[Int],
                    activeExec: Int = 0): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {

        case Tick =>
          context.log.info(s"Tick, queue=${queue}")

          if (queue.isEmpty)
            Behaviors.same
          else {
            queue foreach { target =>
              workers ! Process(target, context.self)
            }
            // reset the Queue and continue...
            apply(workers, replyTo, Seq.empty, results, activeExec + queue.size)
          }

        case Result(number, res) =>
          context.log.info(s"Got result {} => {}", number, res)
          val updatedResult = results ++ res
          val updatedQueue = queue ++ res

          if (updatedQueue.nonEmpty) {
            context.log.info(s"Non Empty queue, awaiting for {} execs!", activeExec)
            context.self ! Tick
            apply(workers, replyTo, updatedQueue, updatedResult, activeExec - 1)
          } else if (activeExec >= 2) {
            context.log.info(s"Empty queue, still awaiting for {} execs!", activeExec)
            apply(workers, replyTo, updatedQueue, updatedResult, activeExec - 1)
          } else {
            context.log.info(s"Empty queue, all done!")
            replyTo ! TreeWalked(updatedResult)
            Behaviors.empty
          }

        case _ =>
          context.log.error("unhandled...")
          Behaviors.unhandled
      }

    }

}


object AkkaApp extends App {

  case object BootCmd

  val root: Behavior[NotUsed] = Behaviors.setup { context =>
    val crawler = context.spawn(TreeWalkerActor(), "Walker")
    val log = context.log

    val responder = context.spawn(Behaviors.receiveMessage[TreeWalked] {
      case TreeWalked(ids) =>
        log.info("I got the IDs: {}", ids)

        context.system.terminate()
        Behaviors.stopped

    }, "responder")

    crawler ! Initialize(api.Root, responder)

    Behaviors.empty
  }

  ActorSystem(root, "root-actor")

}