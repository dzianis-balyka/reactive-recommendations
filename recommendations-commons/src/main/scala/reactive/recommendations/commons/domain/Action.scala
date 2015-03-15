package reactive.recommendations.commons.domain

import scala.collection
import scala.collection.Set

/**
 * Created by denik on 14.03.2015.
 */
case class Action(ts: Long = System.currentTimeMillis(), user: String, item: String, action: String, params: collection.Map[String, String] = Map[String, String]()) {

}
