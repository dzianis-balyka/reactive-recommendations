package reactive.recommendations.commons.frontend

/**
 * Created by denik on 30.03.2015.
 */
case class Recommend(uid: String,
                     limit: Int,
                     offset: Int) {

}

case class Courses(courses: Set[String]) {


}

