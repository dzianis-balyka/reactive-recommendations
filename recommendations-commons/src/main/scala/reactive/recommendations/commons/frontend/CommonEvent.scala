package reactive.recommendations.commons.frontend

import java.util.Date

/**
 * Created by denik on 30.03.2015.
 */
case class CommonEvent(event: String,
                       entityType: String,
                       entityId: String,
                       targetEntityType: String,
                       targetEntityId: String,
                       eventTime: Date,
                       appId: Int,
                       properties: Option[CommonProperties]) {
}

case class CommonProperties(tags: Option[Set[String]], theme: Option[String], gender: Option[String], birthDate: Option[java.sql.Date], region: Option[String], themes: Option[Set[String]]) {

}

case class EventId(eventId: String) {

}
