package reactive.recommendations.commons.domain

import java.text.SimpleDateFormat
import java.util.Date

import org.joda.time.{DateTime, Years}

/**
 * Created by denik on 14.03.2015.
 */
case class User(
                 id: String,
                 sex: Option[String] = None,
                 birthDate: Option[String] = None,
                 geo: Option[String] = None,
                 categories: Option[Set[String]] = None) {

  def age(): Option[Int] = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    var result: Option[Int] = None
    birthDate.map {
      bd =>
        try {
          result = Some(Years.yearsBetween(new DateTime(sdf.parse(bd)), new DateTime(new Date())).getYears)
        }
    }
    result
  }

}