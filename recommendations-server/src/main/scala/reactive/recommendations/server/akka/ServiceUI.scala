package reactive.recommendations.server.akka

import java.util.Date
import java.util.concurrent.Executors

import akka.actor.{Props, Actor}
import org.json4s.{NoTypeHints, Formats}
import org.json4s.jackson.Serialization
import reactive.recommendations.commons.domain.{Recommendation, User, ContentItem, Action}
import reactive.recommendations.server.ElasticServices
import spray.http.{StatusCode, HttpResponse, HttpRequest, MediaTypes}
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._


import spray.routing.HttpService
import spray.routing.directives.DetachMagnet

import scala.concurrent.{Future, ExecutionContext}


/**
 * Created by d_balyka on 19.11.2014.
 */
object ServiceUI {
  def props(): Props = Props(new ServiceUI())
}

class ServiceUI extends Actor with Service {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(managingRoute)
}


trait Service extends HttpService {

  implicit def json4sFormats: Formats = Serialization.formats(NoTypeHints)

  implicit val reco = jsonFormat3(Recommendation)
  implicit val act = jsonFormat6(Action)
  implicit val itm = jsonFormat7(ContentItem)
  implicit val usr = jsonFormat5(User)
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
  val detachEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  val managingRoute =
    path("recommend") {
      get {
        parameter('uid, 'limit.as[Option[Int]]) {
          (uid: String, limit: Option[Int]) =>
            respondWithMediaType(MediaTypes.`application/json`) {
              complete {
                ElasticServices.findItemsForUser(uid, limit).map {
                  items =>
                    Recommendation(uid, items, Map[String, Array[String]]())
                }
              }
            }
        }
      }
    } ~
      path("action") {
        get {
          parameters('ts.as[Option[Long]], 'user, 'item, 'action) {
            (ts: Option[Long], uid: String, item: String, t: String) =>
              detach(detachEc) {
                complete {
                  ElasticServices.indexAction(Action("" + System.currentTimeMillis(), "" + new Date(), uid, item, t)).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
              }
          }
        } ~
          post {
            entity(as[Action]) {
              action =>
                complete {
                  ElasticServices.indexAction(action).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
            }
          }
      } ~
      path("user") {
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
            entity(as[ContentItem]) {
              item =>
                complete {
                  ElasticServices.indexItem(item).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
            }
          }
      } ~
      path("item") {
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
            entity(as[ContentItem]) {
              item =>
                complete {
                  ElasticServices.indexItem(item).map {
                    ir =>
                      StatusCode.int2StatusCode(200)
                  }
                }
            }
          }
      }

}


