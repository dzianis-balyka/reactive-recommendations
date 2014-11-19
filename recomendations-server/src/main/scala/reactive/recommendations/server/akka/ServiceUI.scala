package reactive.recommendations.server.akka

import akka.actor.{Props, Actor}
import spray.http.{StatusCodes, StatusCode, MediaTypes}
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


trait Service extends HttpService {
  val managingRoute =
    path("recommend") {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            "recommend"
          }
        }
      }
    }
  path("itemViewed") {
    post {
      respondWithStatus(200) {
        complete{
          ""
        }
      }
    }
  }
  path("user") {
    post {
      respondWithStatus(200) {
        complete{
          ""
        }
      }
    }
  }
  path("item") {
    post {
      respondWithStatus(200) {
        complete{
          ""
        }
      }
    }
  }
}