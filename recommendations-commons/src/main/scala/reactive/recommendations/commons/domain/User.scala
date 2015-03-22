package reactive.recommendations.commons.domain

import java.util.Date

/**
 * Created by denik on 14.03.2015.
 */
case class User(
                 id: String,
                 sex: Option[String] = None,
                 birthDate: Option[String] = None,
                 geo: Option[String] = None,
                 categories: Option[Set[String]] = None)