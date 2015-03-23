package reactive.recommendations.commons.domain

import java.text.SimpleDateFormat
import java.util.Date

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
  def tsAsDate(): Option[Date] = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS")
    var result: Option[Date] = None
    try {
      result = Some(sdf.parse(ts))
    }
    result
  }
}
