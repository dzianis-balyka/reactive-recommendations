package reactive.recommendations.server

import java.util.concurrent.Executors

import org.elasticsearch.action.index.IndexResponse
import org.slf4j.LoggerFactory
import reactive.recommendations.server.akka.{User, Action, Item}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient

import scala.concurrent.{ExecutionContext, Future}


/**
 * Created by denik on 30.11.2014.
 */
object ElasticServices {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  val client = ElasticClient.remote("localhost" -> 9300)

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  def indexAction(action: Action): Future[IndexResponse] = client.execute {
    index into "actions/action" id action.id() doc action
  }

  def indexItem(item: Item): Future[IndexResponse] = client.execute {
    index into "items/item" id item.id doc item
  }

  def indexUser(user: User): Future[IndexResponse] = client.execute {
    index into "users/user" id user.id doc user
  }

  def findItemsForUser(userId: String): Future[Array[String]] = {


    client.execute {
      search in "items" types "item" fields ("id") query {
        "*:*"
      }
    }.map {
      sr =>
        log.info("sr.getHits" + sr.getHits.hits())
        sr.getHits.hits().map {
          hit =>
            log.info("hit=" + hit.fields())
            hit.field("_id").getValue
        }
    }
  }


  def main(args: Array[String]) = {
    log.info("" + client.execute {
      search in "items" types "item" fields ("id") query {
        termsQuery("tags", "4", "3").minimumShouldMatch(1)
      } filter {
        termsFilter("tags", "3", "4")
      }
    }.await)
    log.info("" + findItemsForUser("u1").await.toBuffer)


  }

}
