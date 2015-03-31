package reactive.recommendations.server

import java.util.Date

import _root_.akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticClient
import org.json4s.jackson.Serialization
import reactive.recommendations.commons.domain.{ContentItem, Action, Recommendation}
import reactive.recommendations.commons.frontend.CommonEvent
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

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  def main(args: Array[String]) = {
    log.info("client")

    import reactive.recommendations.commons.frontend._

    implicit val system = ActorSystem("ClientRecommendationSystem")
    import system.dispatcher
    // execution context for futures


    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

    val event = CommonEvent("evt", "et", "id", "tet", "tei", new Date(), 1, None)

    log.info("{}", Serialization.writePretty(event))

    val response: Future[HttpResponse] = pipeline(Post("http://localhost:8989/events.json", event))

    log.info("" + response.await)
  }


}
