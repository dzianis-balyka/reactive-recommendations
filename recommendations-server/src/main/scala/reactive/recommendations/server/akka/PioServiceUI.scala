package reactive.recommendations.server.akka

import java.sql
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors

import akka.actor.{Actor, Props}
import org.json4s.{NoTypeHints, Formats}
import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory
import reactive.recommendations.commons.domain.{User, ContentItem, Action, Recommendation}
import reactive.recommendations.server.ElasticServices
import spray.http.{StatusCode, MediaTypes}
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import spray.json.{JsNull, JsString, JsValue, JsonFormat}
import spray.routing.HttpService

import scala.concurrent.ExecutionContext

/**
 * Created by denik on 30.03.2015.
 */
object PioServiceUI {
  def props(): Props = Props(new PioServiceUI())
}

class PioServiceUI extends Actor with PIOService {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(managingRoute)
}

trait PIOService extends HttpService {

  val serviceTraitLogger = LoggerFactory.getLogger(classOf[Service])

  import reactive.recommendations.commons.frontend._

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
  val detachEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  val timestampFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSS"
  val dateFormat = "yyyy-MM-dd"


  def uuid = java.util.UUID.randomUUID.toString

  val managingRoute =
    path("events.json") {
      post {
        entity(as[CommonEvent]) {
          item =>
            serviceTraitLogger.info("{}", item)

            val tsSdf = new SimpleDateFormat(timestampFormat)
            val dateSdf = new SimpleDateFormat(dateFormat)

            (item.event, item.entityType) match {
              case ("$set", "pio_user") => {
                complete {
                  ElasticServices.indexUser(User(item.entityId, item.properties.flatMap(_.gender), item.properties.flatMap(_.birthDate.map(dateSdf.format(_))),
                    item.properties.flatMap(_.region), item.properties.flatMap(_.themes))).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
              }
              case ("$delete", "pio_user") => {
                //do nothing
                complete {
                  StatusCode.int2StatusCode(200)
                }
              }
              case (e: String, "pio_user") => {
                respondWithMediaType(MediaTypes.`application/json`) {
                  val id = uuid
                  complete {
                    ElasticServices.indexActionLogic(Action(id, tsSdf.format(item.eventTime), item.entityId, item.targetEntityId.get, item.event)).map {
                      ir =>
                        EventId(id)
                    }
                  }
                }

              }
              case ("$set", "pio_item") => {
                //                  respondWithMediaType(MediaTypes.`application/json`) {
                complete {
                  ElasticServices.indexItem(ContentItem(item.entityId, Some(tsSdf.format(item.eventTime)), item.properties.flatMap(_.tags), item.properties.flatMap(_.theme.map(Set[String](_))))).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
                //                  }
              }
              case ("$delete", "pio_item") => {
                //do nothing
                complete {
                  StatusCode.int2StatusCode(200)
                }
              }
              case _ => {
                serviceTraitLogger.warn("unknown event " + item)
                complete {
                  StatusCode.int2StatusCode(404)
                }
              }
            }

        }
      }
    } ~ path("queries.json") {
      post {
        entity(as[Recommend]) {
          item =>
            serviceTraitLogger.info("{}", item)
            respondWithMediaType(MediaTypes.`application/json`) {
              complete {
                ElasticServices.recommend(item.uid, item.offset, item.limit).map {
                  items =>
                    Courses(items.toSet)
                }
              }
            }
        }
      }
    }


}