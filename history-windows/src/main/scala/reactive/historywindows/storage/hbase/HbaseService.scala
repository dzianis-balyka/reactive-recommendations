package reactive.historywindows.storage.hbase

import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{client, HBaseConfiguration}
import org.apache.hadoop.hbase.client.{Put, HTable}
import org.slf4j.LoggerFactory

/**
 * Created by denik on 07.03.2015.
 */
object HbaseService {

  val log = LoggerFactory.getLogger(HbaseService.getClass)

  def main(args: Array[String]) = {

    val cfg = HBaseConfiguration.create()
    cfg.set("hbase.zookeeper.quorum", "192.168.175.132")

    val table = new HTable(cfg, "t1")
    val put = new Put(Bytes.toBytes("k2"))
    put.add(Bytes.toBytes("f"), Bytes.toBytes("111"), Bytes.toBytes("www"))
    log.info("" + put)
    table.setOperationTimeout(10)
    log.info("" + table.getOperationTimeout)
    table.put(put)
    log.info("" + put)
    table.flushCommits()
    log.info("commited")
    table.close()

  }

}
