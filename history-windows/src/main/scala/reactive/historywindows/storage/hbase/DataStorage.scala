package reactive.historywindows.storage.hbase

import java.util.Date

import org.apache.commons.codec.digest.DigestUtils
import org.apache.hadoop.hbase.client.Increment
import org.apache.hadoop.hbase.util.Bytes
import org.slf4j.LoggerFactory
import reactive.recommendations.commons.domain.{ContentItem, Action}

/**
 * Created by denik on 13.03.2015.
 */
object DataStorage {

  val log = LoggerFactory.getLogger(DataStorage.getClass)

  var storage: Option[DataStorage] = None

  def setup(hs: HbaseService): Unit = {
    storage = Some(new DataStorage(hs))
  }


  def main(args: Array[String]): Unit = {
    HbaseService.setup("u14lts", "rr_interval_data", "rr_users", "rr_content")
    DataStorage.setup(HbaseService.service.get)

    DataStorage.storage.get.registerAction(Action(user = "uid", item = "i1", action = "view"))
    DataStorage.storage.get.updateIntervalData(user = "uid", categoriesCounters = Map("cat1" -> 12, "cat3" -> 24), tagsCounters = Map("tag1" -> 2, "tag3" -> 4))

  }

}

class DataStorage(hs: HbaseService) {

  val log = LoggerFactory.getLogger(classOf[DataStorage])


  def registerAction(action: Action): Unit = {
    findContentItem(action.item).map {
      ci =>
        updateIntervalData(action.user, ci.categories.map((_, 1)).toMap, ci.tags.map((_, 1)).toMap)
        ci.categories
        ci.tags
    }
  }


  def findContentItem(id: String): Option[ContentItem] = {
    var result: Option[ContentItem] = None
    result
  }


  def updateIntervalData(user: String, categoriesCounters: Map[String, Int], tagsCounters: Map[String, Int], timestamp: Date = new Date()): Unit = {

    hs.doWithIntervalTable {
      it =>
        val incr = new Increment(DigestUtils.md5(user))
        val prefixes = HbaseService.INTERVAL_DATA_PREFIXES.map {
          idp =>
            (idp._1 -> idp._2(timestamp))
        }

        categoriesCounters.foreach {
          cc =>
            prefixes.foreach {
              pref =>
                incr.addColumn(pref._1, Bytes.toBytes("c#%1$s#%2$s".format(pref._2, cc._1)), cc._2.toLong)
            }
        }

        tagsCounters.foreach {
          cc =>
            prefixes.foreach {
              pref =>
                incr.addColumn(pref._1, Bytes.toBytes("t#%1$s#%2$s".format(pref._2, cc._1)), cc._2.toLong)
            }
        }

        it.increment(incr)
        it.flushCommits()

    }

  }

}