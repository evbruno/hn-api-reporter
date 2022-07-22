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

  /**
   * This is the top level "controller", loads the Story and its "kids"
   *
   * @param id
   * @return
   */
  def fetchStory(id: ID): Option[Story] =
    fetchItemResponseStory(id) map { item =>
      val comments = fetchAllComments(item)
      Story(item.id, item.title.get, comments) // FIXME better handling option
    }

  /**
   *
   * @param story
   * @return
   */
  private def fetchAllComments(story: ItemResponse): Map[ID, Comment] = {
    val queue = mutable.Queue[Long]()
    queue.enqueueAll(story.kids.get) // FIXME better handling option

    val comments = Map.newBuilder[ID, Comment]

    while (queue.nonEmpty) {
      val nextId = queue.dequeue()
      val item = service.fetchItem(nextId).get // FIXME better handling option

      item.kids.filter(_.nonEmpty).foreach { kids =>
        queue.enqueueAll(kids)
      }

      if (!item.deleted.getOrElse(false)) {
        comments += item.id ->
          Comment(item.id, item.parent.get, item.by.get, item.kids.getOrElse(Seq.empty))
          // FIXME better handling option
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
