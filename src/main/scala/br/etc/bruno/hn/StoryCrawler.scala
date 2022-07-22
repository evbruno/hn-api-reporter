package br.etc.bruno.hn

import br.etc.bruno.hn.api._
import br.etc.bruno.hn.model.{ Comment, ID, Story }
import scala.collection.mutable


/**
 * Traverses and builds the whole story tree in memory.
 *
 * @param id
 * @param service
 */
class StoryCrawler(implicit val service: HackerNewsAPI.Service) {

  private val noKids = Map.empty[ID, Comment]

  /**
   * This is the top level "controller", loads the Story and its "kids"
   *
   * @param id
   * @return
   */
  def fetchStory(id: ID): Option[Story] =
    fetchItemResponseStory(id) map { item =>
      val comments = item.kids.filter(_.nonEmpty).fold(noKids)(_ => fetchAllComments(item))
      Story(item.id, item.title.getOrElse("<no title>"), comments)
    }

  /**
   *
   * @param story
   * @return
   */
  private def fetchAllComments(story: ItemResponse): Map[ID, Comment] = {
    val queue = mutable.Queue[Long]()
    queue.enqueueAll(story.kids.get)

    val comments = Map.newBuilder[ID, Comment]

    while (queue.nonEmpty) {
      val nextId = queue.dequeue()

      service.fetchItem(nextId).foreach { item =>
        item.kids.filter(_.nonEmpty).foreach { kids =>
          queue.enqueueAll(kids)
        }

        if (!item.deleted.getOrElse(false)) {
          (for {
            parent <- item.parent
            author <- item.by
            kids <- item.kids.orElse(Some(Seq.empty))
          } yield Comment(item.id, parent, author, kids)) foreach { comment =>
            comments += item.id -> comment
          }
        }
      }
    }

    comments.result()
  }

  private def fetchItemResponseStory(id: ID): Option[ItemResponse] =
    service fetchItem id match {
      case r@Some(item) if item.`type` == "story" => r
      case _                                      => None
    }

}
