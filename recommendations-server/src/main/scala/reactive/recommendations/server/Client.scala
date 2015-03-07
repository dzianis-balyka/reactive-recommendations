package reactive.recommendations.server

import _root_.akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
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
import com.sksamuel.elastic4s.ElasticDsl._


import scala.concurrent.Future

/**
 * Created by d_balyka on 28.11.2014.
 */
object Client {

  implicit val reco = jsonFormat2(Recommendation)
  implicit val act = jsonFormat5(Action)
  implicit val itm = jsonFormat4(Item)

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  def main(args: Array[String]) = {
    log.info("client")

    implicit val system = ActorSystem("ClientRecommendationSystem")
    import system.dispatcher
    // execution context for futures


    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val action = Action(System.currentTimeMillis(), "i1", "u22", "view")

    val response: Future[HttpResponse] = pipeline(Post("http://localhost:9200/actions/action/%1$s".format(action.id()), action))

    response.onComplete {
      t =>
        t.map {
          r =>
            log.info("response" + r)
        }
    }


    val client = ElasticClient.remote("localhost" -> 9300)
    val resp = client.execute {
      search in "actions" types ("action") query {
        "*:*"
      }
    }.await

    log.info("" + resp)
  }


}
