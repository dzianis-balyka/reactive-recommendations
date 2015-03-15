package reactive.recommendations.commons.domain

import scala.collection

/**
 * Created by denik on 14.03.2015.
 */
case class ContentItem(id: String, createdTs: Long = System.currentTimeMillis(), tags: collection.Set[String] = Set[String](), categories: Set[String] = Set[String]()) {

}
