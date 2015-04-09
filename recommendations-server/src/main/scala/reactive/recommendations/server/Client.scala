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

    val event = CommonEvent("evt", "et", "id", Some("tet"), Some("tei"), new Date(), 1, None)

    log.info("{}", Serialization.writePretty(event))

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = "http://localhost:8989/events.json",
      entity = HttpEntity(ContentTypes.`application/json`,
        """{
               "event":"$set",
               "entityType":"pio_user",
               "entityId":"7baa56fe-c844-48d9-878c-753d9f449213",
               "properties":
                  {
                    "gender":"m",
                    "birthDate":"01.01.1900",
                    "region":"za",
                    "themes":["personal-development","music"]
                  },
               "eventTime":"2015-03-12T19:26:42.328Z",
               "appId":1
               }""".stripMargin))


    //    val request = HttpRequest(
    //      method = HttpMethods.POST,
    //      uri = "http://localhost:8989/events.json",
    //      entity = HttpEntity(ContentTypes.`application/json`,
    //        """{"event":"buy","entityType":"pio_user","entityId":"7baa56fe-c844-48d9-878c-753d9f449211","targetEntityType":"pio_item","targetEntityId":"23925421","eventTime":"2015-03-12T19:30:48.713Z","appId":1}""".stripMargin))

//    val request = HttpRequest(
//      method = HttpMethods.POST,
//      uri = "http://localhost:8989/queries.json",
//      entity = HttpEntity(ContentTypes.`application/json`,
//        """{"uid":"7baa56fe-c844-48d9-878c-753d9f449211","limit":9,"offset":0}""".stripMargin))

    //    val request = HttpRequest(
    //      method = HttpMethods.POST,
    //      uri = "http://localhost:8989/events.json",
    //      entity = HttpEntity(ContentTypes.`application/json`,
    //        """{"event":"$set","entityType":"pio_item","entityId":"23925421","properties":{"tags":["2d","3d","4d"],"theme":"games"},"eventTime":"2015-03-12T19:26:42.328Z","appId":1}""".stripMargin))


    log.info("" + request)


    //    val response: Future[HttpResponse] = pipeline(Post("http://localhost:8989/events.json", event))
    val response: Future[HttpResponse] = pipeline(request)

    log.info("" + response.await)
  }


}
