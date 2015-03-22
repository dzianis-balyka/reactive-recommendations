package reactive.recommendations.commons.domain

import scala.collection
import scala.collection.Set

/**
 * Created by denik on 14.03.2015.
 */
case class Action(
                   id: String,
                   ts: String,
                   user: String,
                   item: String,
                   actionType: String,
                   params: Map[String, String] = Map[String, String]()) {
}
