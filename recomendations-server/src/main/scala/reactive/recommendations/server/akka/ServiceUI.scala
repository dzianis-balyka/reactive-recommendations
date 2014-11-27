package reactive.recommendations.server.akka

import akka.actor.{Props, Actor}
import spray.http.{MediaTypes}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._


import spray.routing.HttpService

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

case class Action(user: String, item: String, action: String, params: Map[String, String])

case class Item(id: String, tags: Set[String], categories: Set[String])

case class User(id: String)


trait Service extends HttpService {

  val managingRoute =
    path("recommend") {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            implicit val reco = jsonFormat2(Recommendation)
            Recommendation("sss", Array("i1", "i2"))
          }
        }
      }
    } ~
      path("action") {
        (get | post) {
          respondWithStatus(200) {
            complete {
              "hi!!!!!!!!!!"
            }
          }
        }
      } ~
      path("user") {
        post {
          respondWithStatus(200) {
            complete {
              "user"
            }
          }
        }
      } ~
      path("item") {
        post {
          respondWithStatus(200) {
            complete {
              "item"
            }
          }
        }
      }
}


