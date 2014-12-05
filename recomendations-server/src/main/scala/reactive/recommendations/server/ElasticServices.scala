package reactive.recommendations.server

import java.util.concurrent.Executors

import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.search.sort.SortOrder
import org.slf4j.LoggerFactory
import reactive.recommendations.server.akka.{User, Action, Item}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.ElasticClient
import collection.JavaConversions._

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

  def findItemIds(userId: String): Future[Set[String]] = {
    client.execute {
      search in "actions" types "action" fields ("item") query {
        "*:*"
      } filter {
        termFilter("user", userId)
      } sort {
        by field ("ts") order SortOrder.DESC
      }
    }.map {
      sr =>
        sr.getHits.hits().map {
          hit =>
            val itemValue = hit.field("item")
            val item = if (itemValue != null) {
              itemValue.getValue[String]
            } else {
              ""
            }
            item
        }.filter(!_.isEmpty).toSet
    }
  }

  def findCategoriesTags(userId: String): Future[UserPreferences] = {
    findItemIds(userId).flatMap {
      ids =>

        log.info("ids=" + ids)

        client.execute {
          search in "items" types "item" fields("tags", "categories") query {
            "*:*"
          } filter {
            idsFilter(ids.toArray: _*)
          } sort {
            by field ("createdTs") order SortOrder.DESC
          }
        }.map {
          sr =>
            val tc = sr.getHits.hits().map {
              hit =>
                val categoriesValue = hit.field("categories")
                val cats: Array[String] = if (categoriesValue != null) {
                  categoriesValue.getValues.map(_.toString).toArray
                } else {
                  Array[String]()
                }
                val tagsValue = hit.field("tags")
                val tags: Array[String] = if (tagsValue != null) {
                  tagsValue.getValues.map(_.toString).toArray
                } else {
                  Array[String]()
                }
                (cats, tags)
            }

            UserPreferences(tc.flatMap(_._1).toSet, tc.flatMap(_._2).toSet, ids)
        }
    }
  }

  def findItemsForUser(userId: String): Future[Array[String]] = {

    findCategoriesTags(userId).flatMap {
      up =>

        log.info("" + up)

        client.execute {
          search in "items" types "item" fields ("id") filter {
            not {
              idsFilter(up.ids.toArray: _*)
            }
          } query {
            bool {
              should {
                termsQuery("tags", up.tags.toArray: _*).boost(100)
                termsQuery("categories", up.categories.toArray: _*).boost(50)
              }
            }
          } sort {
            by field ("createdTs") order SortOrder.DESC
          }
        }.map {
          sr =>
            sr.getHits.hits().map {
              hit =>
                val fieldValue = hit.field("id")
                if (fieldValue != null) {
                  fieldValue.getValue
                } else {
                  ""
                }
            }.filter(!_.isEmpty)
        }
    }

  }


  def main(args: Array[String]) = {
    log.info("" + client.execute {
      update()
    }.await)
    log.info("" + findItemsForUser("u1").await.toBuffer)

  }

  def main1(args: Array[String]) = {
    log.info("" + client.execute {
      search in "items" types "item" fields("categories", "tags") query {
        "*:*"
      } filter {
        idsFilter("i1", "i2")
      }
    }.await)
    log.info("" + findItemsForUser("u1").await.toBuffer)

  }

}

case class UserPreferences(categories: Set[String], tags: Set[String], ids: Set[String])