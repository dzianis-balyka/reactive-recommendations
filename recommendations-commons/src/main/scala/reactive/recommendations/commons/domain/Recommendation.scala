package reactive.recommendations.commons.domain

/**
 * Created by denik on 20.03.2015.
 */
case class Recommendation(user: String, items: Array[String], tops: Map[String, Array[String]]) {

}
