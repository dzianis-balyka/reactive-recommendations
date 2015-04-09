package reactive.recommendations.commons

import java.sql
import java.text.SimpleDateFormat
import java.util.Date

import org.json4s.{NoTypeHints, Formats}
import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsString, JsValue, JsonFormat}

/**
 * Created by denik on 31.03.2015.
 */
package object frontend {

  val packageLogger = LoggerFactory.getLogger("reactive.recommendations.commons.frontend._")

  implicit def json4sFormats: Formats = Serialization.formats(NoTypeHints)

  implicit val dateFormat = new JsonFormat[java.sql.Date] {

    val dateFmt = "dd.MM.yyyy"

    def read(json: JsValue): java.sql.Date = {
      val dateString = json.asInstanceOf[JsString].value
      var result: java.sql.Date =
        try {
          sql.Date.valueOf(json.asInstanceOf[JsString].value)
        } catch {
          case t: Throwable => {
            packageLogger.warn("{}", t)
            new java.sql.Date(new SimpleDateFormat(dateFmt).parse(dateString).getTime)
          }
        }
      result
    }

    def write(date: java.sql.Date) =
      JsString(date.toString)
  }
  implicit val dateTimeFormat = new JsonFormat[Date] {
    val fmt = "yyyy-MM-dd'T'HH:mm:ss.SSSSS"

    def read(json: JsValue): Date = {
      val sdf = new SimpleDateFormat(fmt)
      sdf.parse(json.asInstanceOf[JsString].value)
    }

    def write(date: Date) = {
      val sdf = new SimpleDateFormat(fmt)
      JsString(sdf.format(date))
    }
  }
  implicit val commonPropertiesFormat = jsonFormat6(CommonProperties)
  implicit val commonEventFormat = jsonFormat8(CommonEvent)
  implicit val eventFormat = jsonFormat1(EventId)
  implicit val recommendFormat = jsonFormat3(Recommend)
  implicit val coursesFormat = jsonFormat1(Courses)

}
