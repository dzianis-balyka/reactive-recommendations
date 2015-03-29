package reactive.recommendations.server

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.{TimeUnit, Executors}

import com.sksamuel.elastic4s.mappings.FieldType.{LongType, DateType, StringType}
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.{UpdateResponse, UpdateRequest}
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.get.GetField
import org.elasticsearch.search.{SearchHitField, SearchHit}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import reactive.recommendations.commons.domain._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{QueryDefinition, ElasticClient}
import collection.JavaConversions._
import scala.concurrent.duration.Duration

import scala.concurrent.{ExecutionContext, Future}


/**
 * Created by denik on 30.11.2014.
 */
object ElasticServices {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(10))

  //    val client = ElasticClient.remote("predictio8.hcl.pp.coursmos.com" -> 9300)
  val clusterSettings = ImmutableSettings.settingsBuilder().put("cluster.name", "xyz").build()
  val client = ElasticClient.remote(/*clusterSettings,*/ "localhost" -> 9300)
  val defaultLimit = 100
  val defaultInternalLimit = 1000

  val log = LoggerFactory.getLogger(ServerRunner.getClass)

  val incrementTemplate =
    """
      |if(!ctx._source.containsKey('%1$s')){
      | ctx._source.%1$s = %2$s
      |} else {
      | ctx._source.%1$s += %2$s
      |}
    """.stripMargin

  val putTemplate =
    """
      |if(!ctx._source.containsKey('%1$s')){
      | ctx._source.%1$s = %2$s
      |}
    """.stripMargin


  def createProfileIndex(): Future[CreateIndexResponse] = client.execute {
    create index "profiles" shards 5 replicas 1 mappings(
      "user" as(
        "id" typed StringType,
        "sex" typed StringType,
        "birthDate" typed DateType,
        "geo" typed StringType,
        "categories" typed StringType,
        "l10n" typed StringType
        ),

      "intervalInterest" as(
        "id" typed StringType,
        "user" typed StringType,
        "interval" typed StringType,
        "sex" typed StringType,
        "geo" typed StringType,
        "age" typed StringType,
        "l10n" typed StringType,
        "intervalDate" typed DateType,
        "actionsCount" typed LongType
        )

      )
  }


  def createItemIndex(): Future[CreateIndexResponse] = client.execute {
    create index "items" shards 5 replicas 1 mappings(
      "item" as(
        "id" typed StringType,
        "version" typed DateType,
        "author" typed StringType,
        "tags" typed StringType,
        "terms" typed StringType,
        "categories" typed StringType,
        "l10n" typed StringType
        ),
      "itemInterest" as(
        "id" typed StringType,
        "author" typed StringType,
        "tags" typed StringType,
        "terms" typed StringType,
        "categories" typed StringType,
        "l10n" typed StringType,
        "intervalDate" typed DateType,
        "actionsCount" typed LongType
        )
      )

  }

  def createActionIndex(): Future[CreateIndexResponse] = client.execute {
    create index "actions" shards 7 replicas 1 mappings {
      "action" as(
        "id" typed StringType,
        "item" typed StringType,
        "user" typed StringType,
        "type" typed StringType,
        "ts" typed DateType
        )
    }
  }

  def readUser(fields: Map[String, AnyRef]): User = {
    User(extractValue(fields, "id").get, extractValue(fields, "sex"), extractValue(fields, "birthDate"), extractValue(fields, "geo"), extractValues(fields, "l10n"))
  }

  def readContentItem(fields: Map[String, AnyRef]): ContentItem = {
    ContentItem(extractValue(fields, "id").get, extractValue(fields, "ts"), extractValues(fields, "tags"), extractValues(fields, "categories"), extractValues(fields, "terms"), extractValue(fields, "author"), extractValues(fields, "l10n"))
  }

  def readUserIterest(fields: Map[String, AnyRef]): UserInerest = {


    var tags = collection.mutable.Map[String, Int]()
    var terms = collection.mutable.Map[String, Int]()
    var categories = collection.mutable.Map[String, Int]()


    val tagsR = "tg_(.*)".r
    val termsR = "t_(.*)".r
    val cateroriesR = "c_(.*)".r

    fields.foreach {
      x =>
        x._1 match {
          case tagsR(n) => tags += (n -> x._2.toString.toInt)
          case termsR(n) => terms += (n -> x._2.toString.toInt)
          case cateroriesR(n) => categories += (n -> x._2.toString.toInt)
          case _ => {}
        }
    }

    log.info("{}", fields)

    UserInerest(if (extractValue(fields, "id").get.endsWith("_total")) {
      None
    } else {
      extractValue(fields, "ts")
    }, categories, tags, terms, extractValue(fields, "actionsCount").get.toInt)
  }


  def indexActionLogic(action: Action): Future[Option[BulkResponse]] = {


    client.execute {
      multiget(
        get id action.user from "profiles/user",
        get id action.item from "items/item"
      )
    }.flatMap {
      mgr =>
        if (mgr.getResponses()(0).getResponse.isExists && mgr.getResponses()(1).getResponse.isExists) {
          val item = readContentItem(mgr.getResponses()(1).getResponse.getSource.toMap)
          val user = readUser(mgr.getResponses()(0).getResponse.getSource.toMap)

          log.info("" + item)
          log.info("" + user)

          indexMonthlyInterest(action, user, item).map {
            br =>
              Some(br)
          }
        } else {
          Future {
            None
          }
        }
    }

  }


  def indexAction(action: Action): Future[IndexResponse] = client.execute {
    val docFields = collection.mutable.Map[String, Any]()
    docFields += ("id" -> action.id)
    docFields += ("item" -> action.item)
    docFields += ("user" -> action.user)
    docFields += ("type" -> action.actionType)
    docFields += ("ts" -> action.ts)
    index into "actions/action" id (action.id) fields (docFields)
  }

  def indexMonthlyInterest(action: Action, user: User, item: ContentItem): Future[BulkResponse] = {
    val sdf = new SimpleDateFormat("yyyy-MM")
    val docFields = collection.mutable.Map[String, Any]()
    val docId = "%1$s_%2$s".format(user.id, sdf.format(action.tsAsDate().getOrElse(new Date())))
    val docIdTotal = "%1$s_total".format(user.id)

    docFields += ("id" -> docId)
    docFields += ("user" -> action.user)
    docFields += ("intervalDate" -> sdf.format(action.tsAsDate().getOrElse(new Date())))
    user.age().map {
      v =>
        docFields += ("age" -> v)
    }
    user.categories.map {
      v =>
        docFields += ("categories" -> v.toArray)
    }
    user.geo.map {
      v =>
        docFields += ("geo" -> v)
    }
    user.sex.map {
      v =>
        docFields += ("sex" -> v)
    }

    val deltaScript = new StringBuilder()
    val deltaParams = collection.mutable.Map[String, Any]()

    appendToScript(deltaScript, deltaParams, Some(docFields), false, "actionsCount", "ac", 1)

    item.categories.foreach {
      catOption =>
        catOption.foreach {
          cat =>
            appendToScript(deltaScript, deltaParams, Some(docFields), false, "c_%1$s".format(cat), "p_c_%1$s".format(cat), 1)
        }
    }

    item.tags.foreach {
      catOption =>
        catOption.foreach {
          cat =>
            appendToScript(deltaScript, deltaParams, Some(docFields), false, "tg_%1$s".format(cat), "p_tg_%1$s".format(cat), 1)
        }
    }

    item.terms.foreach {
      catOption =>
        catOption.foreach {
          cat =>
            appendToScript(deltaScript, deltaParams, Some(docFields), false, "t_%1$s".format(cat), "p_t_%1$s".format(cat), 1)
        }
    }


    log.info("" + deltaScript.toString())
    log.info("" + deltaParams)


    val itemDocId = "%1$s_%2$s".format(item.id, sdf.format(action.tsAsDate().getOrElse(new Date())))
    val itemDocIdTotal = "%1$s_total".format(item.id)
    val itemDocFields = collection.mutable.Map[String, Any]()

    itemDocFields += ("item" -> action.item)
    itemDocFields += ("id" -> itemDocId)
    itemDocFields += ("intervalDate" -> sdf.format(action.tsAsDate().getOrElse(new Date())))
    item.author.map {
      v =>
        itemDocFields += ("author" -> v)
    }
    item.categories.map {
      v =>
        itemDocFields += ("categories" -> v.toArray)
    }
    item.tags.map {
      v =>
        itemDocFields += ("tags" -> v.toArray)
    }
    item.terms.map {
      v =>
        itemDocFields += ("terms" -> v.toArray)
    }
    item.l10n.map {
      v =>
        itemDocFields += ("l10n" -> v)
    }

    val itemDeltaScript = new StringBuilder()
    val itemDeltaParams = collection.mutable.Map[String, Any]()
    appendToScript(deltaScript, deltaParams, Some(itemDocFields), false, "actionsCount", "ac", 1)
    appendToScript(deltaScript, deltaParams, Some(itemDocFields), false, "actionsCount_%1$s".format(action.actionType), "act", 1)
    user.age().map {
      v =>
        appendToScript(itemDeltaScript, itemDeltaParams, Some(itemDocFields), false, "a_%1$s".format(v), "p_a_%1$s".format(v), 1)
    }
    user.geo.map {
      v =>
        appendToScript(itemDeltaScript, itemDeltaParams, Some(itemDocFields), false, "g_%1$s".format(v), "p_g_%1$s".format(v), 1)
    }
    user.sex.map {
      v =>
        appendToScript(itemDeltaScript, itemDeltaParams, Some(itemDocFields), false, "s_%1$s".format(v), "p_s_%1$s".format(v), 1)
    }


    val actionDocFields = collection.mutable.Map[String, Any]()
    actionDocFields += ("id" -> action.id)
    actionDocFields += ("item" -> action.item)
    actionDocFields += ("user" -> action.user)
    actionDocFields += ("type" -> action.actionType)
    actionDocFields += ("ts" -> action.ts)

    client.execute {
      bulk(
        index into "actions/action" id (action.id) fields (actionDocFields),
        update(docId) in ("profiles/intervalInterest") script (deltaScript.toString()) params (deltaParams.toMap) upsert (docFields),
        update(docIdTotal) in ("profiles/intervalInterest") script (deltaScript.toString()) params (deltaParams.toMap) upsert (docFields ++ Map("id" -> docIdTotal, "total" -> "true")),
        update(itemDocId) in ("items/itemInterest") script (itemDeltaScript.toString()) params (itemDeltaParams.toMap) upsert (itemDocFields),
        update(itemDocIdTotal) in ("items/itemInterest") script (itemDeltaScript.toString()) params (itemDeltaParams.toMap) upsert (itemDocFields ++ Map("id" -> itemDocIdTotal, "total" -> "true"))
      )
    }
  }


  def appendToScript(script: StringBuilder, params: collection.mutable.Map[String, Any], upsertParams: Option[collection.mutable.Map[String, Any]], isPut: Boolean, property: String, pn: String, value: Any): Unit = {
    script.append(
      (if (isPut) {
        putTemplate
      } else {
        incrementTemplate
      }).format(property, pn))
    params += (pn -> value)
    upsertParams.map {
      p =>
        p += (property -> value)
    }
  }

  def indexItem(item: ContentItem): Future[IndexResponse] = client.execute {
    val docFields = collection.mutable.Map[String, Any]()
    docFields += ("id" -> item.id)
    item.author.map {
      v =>
        docFields += ("author" -> v)
    }
    item.categories.map {
      v =>
        docFields += ("categories" -> v.toArray)
    }
    item.tags.map {
      v =>
        docFields += ("tags" -> v.toArray)
    }
    item.terms.map {
      v =>
        docFields += ("terms" -> v.toArray)
    }
    item.createdTs.map {
      v =>
        docFields += ("version" -> v)
    }
    item.l10n.map {
      v =>
        docFields += ("l10n" -> v)
    }
    index into "items/item" id (item.id) fields (docFields)
  }


  def deleteIndexes(): Unit = {
    log.info("" + client.execute {
      delete index "items"
    }.await)
    log.info("" + client.execute {
      delete index "profiles"
    }.await)
    log.info("" + client.execute {
      delete index "actions"
    }.await)
  }

  def createIndexes(): Unit = {
    log.info("" + createProfileIndex().await)
    log.info("" + createItemIndex().await)
    log.info("" + createActionIndex().await)
  }


  def indexUser(user: User): Future[IndexResponse] = client.execute {
    val docFields = collection.mutable.Map[String, Any]()
    docFields += ("id" -> user.id)
    user.birthDate.map {
      v =>
        docFields += ("birthDate" -> v)
    }
    user.categories.map {
      v =>
        docFields += ("categories" -> v.toArray)
    }
    user.geo.map {
      v =>
        docFields += ("geo" -> v)
    }
    user.sex.map {
      v =>
        docFields += ("sex" -> v)
    }
    log.info("" + docFields)
    index into "profiles/user" id (user.id) fields (docFields)
  }


  def extractValues(fields: collection.Map[String, Any], fn: String): Option[Set[String]] = {
    if (fields.containsKey(fn)) {
      Some(fields.get(fn).get.asInstanceOf[java.util.Collection[Any]].map("" + _).toSet)
    } else {
      None
    }
  }

  def extractValue(fields: collection.Map[String, Any], fn: String): Option[String] = {
    if (fields.containsKey(fn)) {
      Some(fields.get(fn).get.toString)
    } else {
      None
    }
  }


  def findItemIds(userId: String): Future[Set[String]] = {
    client.execute {
      search in "actions" types "action" fields ("item") query {
        "*:*"
      } filter {
        termFilter("user", userId)
      } sort {
        by field ("ts") order SortOrder.DESC
      } limit (defaultInternalLimit)
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
          } limit (defaultInternalLimit)
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


  def findItemsForUser(userId: String, limit: Option[Int] = None): Future[Array[String]] = {

    findCategoriesTags(userId).flatMap {
      up =>

        log.info("" + up)



        client.execute {

          if (up.ids.isEmpty) {
            search in "items" types "item" fields ("id") query {
              "*:*"
            } sort {
              by field ("createdTs") order SortOrder.DESC
            } limit (limit.getOrElse(defaultLimit))
          } else {
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
            } limit (limit.getOrElse(defaultLimit))
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


  def recommend(userId: String, from: Int = 0, limit: Int = 100): Future[Seq[String]] = {

    val sdf = new SimpleDateFormat("yyyy-MM")
    var dt = new DateTime()

    val idUserInterestTotal = "%1$s_total".format(userId)
    val idUserInterestCurrent = "%1$s_%2$s".format(userId, sdf.format(dt.toDate))
    dt = dt.minusMonths(1)
    val idUserInterestPrevious1 = "%1$s_%2$s".format(userId, sdf.format(dt.toDate))
    dt = dt.minusMonths(1)
    val idUserInterestPrevious2 = "%1$s_%2$s".format(userId, sdf.format(dt.toDate))
    dt = dt.minusMonths(1)
    val idUserInterestPrevious3 = "%1$s_%2$s".format(userId, sdf.format(dt.toDate))



    //find user total interests
    client.execute {
      multiget(
        get id userId from "profiles/user",
        get id idUserInterestTotal from "profiles/intervalInterest",
        get id idUserInterestCurrent from "profiles/intervalInterest",
        get id idUserInterestPrevious1 from "profiles/intervalInterest",
        get id idUserInterestPrevious2 from "profiles/intervalInterest",
        get id idUserInterestPrevious3 from "profiles/intervalInterest"
      )
    }.flatMap {
      mgr =>

        val user = readUser(mgr.getResponses()(0).getResponse.getSource.toMap)
        val userInterestTotal = readUserIterest(mgr.getResponses()(1).getResponse.getSource.toMap)
        //        val user = readUser(mgr.getResponses()(2).getResponse.getSource.toMap)
        //        val user = readUser(mgr.getResponses()(3).getResponse.getSource.toMap)

        log.info("{}", userInterestTotal)

        //        if (up.ids.isEmpty) {
        //          search in "items" types "item" fields ("id") query {
        //            "*:*"
        //          } sort {
        //            by field ("createdTs") order SortOrder.DESC
        //          } limit (limit.getOrElse(defaultLimit))
        //        } else {

        val totalItemsCriteria = collection.mutable.Buffer[QueryDefinition]()
        user.sex.map { x => totalItemsCriteria += termsQuery("sex", x)}
        user.geo.map { x => totalItemsCriteria += termsQuery("geo", x)}
        user.age().map { x => totalItemsCriteria += termsQuery("age", "" + x)}

        findItemIds(userId).flatMap {
          idsToFilter =>
            client.execute(
              search in "items" types ("item") fields ("id") query (
                bool(
                  should(
                    (userInterestTotal.categoriesIterest.map {
                      x =>
                        termsQuery("categories", x._1).boost(x._2)
                    } ++
                      userInterestTotal.tagIterest.map {
                        x =>
                          termsQuery("tags", x._1).boost(x._2)
                      } ++
                      userInterestTotal.termsIterest.map {
                        x =>
                          termsQuery("terms", x._1).boost(x._2)
                      } ++ totalItemsCriteria
                      ).toArray: _*
                  )
                )
                ) filter {
                not {
                  idsFilter(idsToFilter.toArray: _*)
                }
              },
              search in "items" types ("itemInterest") fields ("item") query (
                bool(
                  should(
                    totalItemsCriteria.toArray: _*
                  )
                )
                ) filter {
                not {
                  idsFilter(idsToFilter.toArray: _*)
                }
              },
              search in "items" types ("item") fields ("id") query (
                matchall
                ) sort (by field ("createdTs") order SortOrder.DESC) filter {
                not {
                  idsFilter(idsToFilter.toArray: _*)
                }
              }

            ).map {
              msr =>
                msr.getResponses.map(_.getResponse)
                  .filter(_ != null)
                  .filter(_.getHits != null)
                  .filter(!_.getHits.hits().isEmpty)
                  .flatMap {
                  sr =>

                    sr.getHits.hits().map {
                      hit =>
                        val fieldValues = hit.fields()
                        if (fieldValues.contains("id")) {
                          "" + fieldValues.get("id").getValue
                        } else if (fieldValues.contains("item")) {
                          "" + fieldValues.get("item").getValue
                        } else {
                          ""
                        }
                    }.filter(!_.isEmpty)
                }
            }
        }
    }

    //find items total interest
    //filter by user actions

  }


  def main(args: Array[String]) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSS")

    //    deleteIndexes()
    //    createIndexes()

    val u = User("u1", Some("male"), Some("1979-11-29"), Some("Minsk"), Some(Set[String]("cat1", "cat2")))
    val i = ContentItem("ci1", Some("2015-01-01T09:08:07.123"), Some(Set[String]("tag1", "tag2")), Some(Set[String]("cat1", "cat2")), Some(Set[String]("term1", "term2")), Some("author"), None)
    val i2 = ContentItem("ci2", Some("2015-01-01T09:08:07.123"), Some(Set[String]("tag1", "tag3")), Some(Set[String]("cat4", "cat2")), Some(Set[String]("term5", "term2")), Some("author"), None)
    val a1 = Action("a1", sdf.format(new Date()), "u1", "ci1", "view")
    val a2 = Action("a1", sdf.format(new Date()), "u1", "ci1", "view")

    implicit val d = Duration(30, TimeUnit.SECONDS)

    //    log.info("" + indexUser(u).await)
    //        log.info("" + indexItem(i).await)
    log.info("" + indexItem(i2).await)
    //    log.info("" + indexActionLogic(a1).await.map { br => br.buildFailureMessage()}.getOrElse("None"))
    //    log.info("" + indexActionLogic(a2).await.map { br => br.buildFailureMessage()}.getOrElse("None"))

    log.info("" + recommend(u.id).await)

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