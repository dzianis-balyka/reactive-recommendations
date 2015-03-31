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
import reactive.recommendations.commons.frontend._
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

class PioServiceUI extends Actor with Service {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(managingRoute)
}

trait Service extends HttpService {

  val serviceTraitLogger = LoggerFactory.getLogger(classOf[Service])

  import reactive.recommendations.commons.frontend._

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
  val detachEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  val managingRoute =
    path("events.json") {
      get {
        parameters('id, 'ts.as[Option[String]], 'tags.as[Option[String]], 'categories.as[Option[String]], 'terms.as[Option[String]], 'author.as[Option[String]]) {
          (id: String, ts: Option[String], tags: Option[String], categories: Option[String], terms: Option[String], author: Option[String]) =>
            complete {
              ElasticServices.indexItem(ContentItem(id, ts, tags.map(_.split(",").toSet), categories.map(_.split(",").toSet), terms.map(_.split(",").toSet), author)).map {
                ir =>
                  StatusCode.int2StatusCode(200)
              }
            }
        }
      } ~
        post {
          entity(as[CommonEvent]) {
            item =>
              serviceTraitLogger.info("{}", item)
              complete {
                StatusCode.int2StatusCode(200)
              }
          }
        }
    }


}