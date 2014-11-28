package reactive.recommendations.server

import _root_.akka.actor.ActorSystem
import reactive.recommendations.server.akka.{Action, Item, Recommendation}
import spray.http._
import spray.client.pipelining._
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._


import scala.concurrent.Future

/**
 * Created by d_balyka on 28.11.2014.
 */
object Client {

  implicit val reco = jsonFormat2(Recommendation)
  implicit val act = jsonFormat4(Action)
  implicit val itm = jsonFormat3(Item)

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  def main(args: Array[String]) = {
    log.info("client")

    implicit val system = ActorSystem("ClientRecommendationSystem")
    import system.dispatcher
    // execution context for futures


    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val response: Future[HttpResponse] = pipeline(Get("http://localhost:8080/action?user=x&item=ffds&action=23"))

    response.onComplete {
      t =>
        t.map {
          r =>
            log.info("response" + r.entity.as[Action])
        }
    }

  }

}
