package reactive.recommendations.commons.domain

/**
 * Created by denik on 28.03.2015.
 */
case class UserInterest(date: Option[String], categoriesIterest: collection.Map[String, Int], tagIterest: collection.Map[String, Int], termsIterest: collection.Map[String, Int], actionCount: Int) {

}
