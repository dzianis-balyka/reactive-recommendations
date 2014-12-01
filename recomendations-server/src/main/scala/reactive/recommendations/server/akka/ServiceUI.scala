package reactive.recommendations.server.akka

import java.util.concurrent.Executors

import akka.actor.{Props, Actor}
import reactive.recommendations.server.ElasticServices
import spray.client.pipelining._
import spray.http.{StatusCode, HttpResponse, HttpRequest, MediaTypes}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport
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


case class Recommendation(user: String, items: Array[String])

case class Action(ts: Long, user: String, item: String, action: String, params: Map[String, String] = Map[String, String]()) {
  def id(): String = {
    "%1$s-%2$s-%3$s-%4$s".format(ts, user, item, action)
  }
}

case class Item(id: String, createdTs: Long, tags: Set[String] = Set[String](), categories: Set[String] = Set[String]())

case class User(id: String)


trait Service extends HttpService {

  implicit val reco = jsonFormat2(Recommendation)
  implicit val act = jsonFormat5(Action)
  implicit val itm = jsonFormat4(Item)
  implicit val usr = jsonFormat1(User)
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))
  val detachEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  val managingRoute =
    path("recommend") {
      get {
        parameter('uid) {
          uid =>
            respondWithMediaType(MediaTypes.`application/json`) {
              complete {
                ElasticServices.findItemsForUser(uid).map {
                  items =>
                    Recommendation(uid, items)
                }
              }
            }
        }
      }
    } ~
      path("action") {
        get {
          parameters('ts.as[Long], 'user, 'item, 'action) {
            (ts: Long, uid: String, item: String, t: String) =>
              detach(detachEc) {
                complete {
                  ElasticServices.indexAction(Action(ts, uid, item, t)).map {
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
        (get | post) {
          complete {
            StatusCode.int2StatusCode(404)
          }
        }
      } ~
      path("item") {
        get {
          parameters('id, 'ts.as[Long], 'tag, 'category) {
            (id: String, ts: Long, tag: String, category: String) =>
              parameterMultiMap {
                pmp =>
                  complete {
                    ElasticServices.indexItem(Item(id, ts, pmp.get("tag").get.toSet, pmp.get("category").get.toSet)).map {
                      ir =>
                        StatusCode.int2StatusCode(200)
                    }
                  }
              }
          }
        } ~
          post {
            entity(as[Item]) {
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


