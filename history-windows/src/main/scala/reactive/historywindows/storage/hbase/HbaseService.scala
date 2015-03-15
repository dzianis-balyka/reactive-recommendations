package reactive.historywindows.storage.hbase

import java.net
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{client, HBaseConfiguration}
import org.apache.hadoop.hbase.client.{Put, HTable}
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

/**
 * Created by denik on 07.03.2015.
 */
object HbaseService {

  val HOURLY_CF = Bytes.toBytes("h")
  val DAILY_CF = Bytes.toBytes("d")
  val WEEKLY_CF = Bytes.toBytes("w")
  val MONTHLY_CF = Bytes.toBytes("m")
  val TOTAL_CF = Bytes.toBytes("t")
  val ACTION_CF = Bytes.toBytes("a")

  val INTERVAL_DATA_PREFIXES = Map[Array[Byte], (Date => String)](
    HOURLY_CF -> { d =>
      val fmt = new SimpleDateFormat("yyyy-MM-dd HH")
      fmt.format(d)
    },
    DAILY_CF -> { d =>
      val fmt = new SimpleDateFormat("yyyy-MM-dd")
      fmt.format(d)
    },
    WEEKLY_CF -> { d =>
      val fmt = new SimpleDateFormat("yyyy-ww")
      fmt.format(d)
    },
    MONTHLY_CF -> { d =>
      val fmt = new SimpleDateFormat("yyyy-MM")
      fmt.format(d)
    },
    TOTAL_CF -> { d =>
      ""
    }

  )

  val log = LoggerFactory.getLogger(HbaseService.getClass)

  var service: Option[HbaseService] = None

  def setup(zkQuorum: String, intervalDataTable: String, usersTable: String, contentTable: String): Unit = {
    service = Some(new HbaseService(zkQuorum, intervalDataTable, usersTable, contentTable))
  }

  def main(args: Array[String]) = {


    HbaseService.setup("u14lts", "rr_interval_data", "rr_users", "rr_content")
    HbaseService.service.get.doWithIntervalTable {
      table =>
        val put = new Put(Bytes.toBytes("kk2"))
        put.add(Bytes.toBytes("h"), Bytes.toBytes("111"), Bytes.toBytes("www"))
        log.info("" + put)
        table.setOperationTimeout(10)
        table.getRegionLocations.foreach {
          x =>
            log.info("" + x)
        }
        log.info("" + table.getOperationTimeout)
        table.put(put)
        log.info("" + put)
        table.flushCommits()
        log.info("commited")
        table.close()
    }


  }

}


class HbaseService(zkQuorum: String, intervalDataTable: String, usersTable: String, contentTable: String) {

  val log = LoggerFactory.getLogger(classOf[HbaseService])

  val cfg = HBaseConfiguration.create()
  cfg.set("hbase.zookeeper.quorum", zkQuorum)

  def doWithIntervalTable(f: HTable => Unit): Unit = {
    val t = new HTable(cfg, intervalDataTable)
    f(t)
    t.close()
  }

  def doWithUsersTable(f: HTable => Unit): Unit = {
    val t = new HTable(cfg, usersTable)
    f(t)
    t.close()
  }

  def doWithContentTable(f: HTable => Unit): Unit = {
    val t = new HTable(cfg, contentTable)
    f(t)
    t.close()
  }


}