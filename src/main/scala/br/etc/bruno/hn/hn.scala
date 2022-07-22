package br.etc.bruno

package object hn {

  /**
   * Maps the API to domain models, discarding unused fields
   */
  object model {

    // I was not about Int or Long... so Ill be using an alias for now
    // note: the API says the ID is unique
    type ID = Long

    /**
     * Holds the Comment object details
     *
     * @see See [[br.etc.bruno.hn.api.ItemResponse]] for the json API response
     * @param id
     * @param parent
     * @param user
     * @param kids
     */
    case class Comment(
      id: ID,
      parent: ID,
      user: String,
      kids: Seq[ID] = Seq.empty)

    /**
     * Holds the Story object as a simple model.
     *
     * @see See [[br.etc.bruno.hn.api.ItemResponse]] for the json API response
     * @param id
     * @param title
     * @param kids
     */
    case class Story(
      id: ID,
      title: String,
      kids: Map[ID, Comment])
  }

  /**
   * Mapping the API (v0)
   *
   * @see [[https://github.com/HackerNews/API]]
   */
  object api {

    /**
     * Just wrapping the response
     * @param ids
     */
    case class TopStoriesResponse(ids: Seq[Long])

    /**
     * @see
     * @param id The item's unique id.
     * @param `type` The type of item. One of "job", "story", "comment", "poll", or "pollopt".
     * @param by The username of the item's author. If deleted, this field is absent
     * @param text The comment, story or poll text. HTML.
     * @param title The title of the story, poll or job. HTML.
     * @param parent The comment's parent: either another comment or the relevant story.
     * @param kids The ids of the item's comments, in ranked display order.
     * @param deleted True if the item is deleted.
     */
    case class ItemResponse(
       id: Long,
       `type`: String,
       by: Option[String] = None,
       text: Option[String] = None,
       title: Option[String] = None,
       parent: Option[Long] = None,
       kids: Option[Seq[Long]] = None,
       deleted: Option[Boolean] = None
     )
  }

}
