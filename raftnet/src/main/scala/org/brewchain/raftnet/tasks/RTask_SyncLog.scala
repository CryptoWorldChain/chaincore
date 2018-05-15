package org.brewchain.raftnet.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import org.brewchain.raftnet.pbgens.Raftnet.PSJoin
import onight.tfw.outils.serialize.UUIDGenerator
import onight.tfw.async.CallBack
import onight.tfw.otransio.api.beans.FramePacket
import org.brewchain.raftnet.pbgens.Raftnet.PRetJoin
import org.brewchain.raftnet.utils.RConfig
import org.brewchain.raftnet.pbgens.Raftnet.PRaftNode
import org.brewchain.raftnet.pbgens.Raftnet.PSSyncEntries
import org.brewchain.raftnet.tasks.RSM;
import org.brewchain.raftnet.pbgens.Raftnet.PRetSyncEntries

import scala.collection.JavaConversions._
import org.brewchain.raftnet.pbgens.Raftnet.PLogEntry
import org.brewchain.raftnet.Daos
import org.brewchain.bcapi.gens.Oentity.OValue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

//获取其他节点的term和logidx，commitidx
case class RTask_SyncLog(startIdx: Long, endIdx: Long,
    network: Network, fastNodeID: String,
    runCounter: AtomicLong) extends SRunner with LogHelper {
  def getName(): String = "SyncLog:" + startIdx + "-" + (endIdx)

  def runOnce() = {
    //
    try {
      val messageid = UUIDGenerator.generate();
      val sync = PSSyncEntries.newBuilder().setStartIdx(startIdx)
        .setEndIdx(endIdx).setRn(RSM.curRN()).setMessageId(messageid).build()
      val start = System.currentTimeMillis();
      val n = network.nodeByBcuid(fastNodeID);
      if (n == null) {
        log.warn("cannot found node from Network:" + network.netid + ",bcuid=" + fastNodeID)
      }
      network.sendMessage("SYNRAF", sync, n, new CallBack[FramePacket] {
        def onSuccess(fp: FramePacket) = {
          val end = System.currentTimeMillis();
          log.debug("send SYNRAF success:to " + n.uri + ",cost=" + (end - start))
          val ret = PRetSyncEntries.newBuilder().mergeFrom(fp.getBody);
          if (ret.getRetCode() == 0) { //same message
            var maxid: Long = 0
            val realmap = ret.getEntriesList.map { b => (b, PLogEntry.newBuilder().mergeFrom(b)) }
              .filter { p => p._2.getLogIdx >= startIdx && p._2.getLogIdx <= endIdx }
//            if (realmap.size() == endIdx - startIdx + 1) {
              realmap.map { p =>
                val b = p._1;
                val loge = p._2;
                log.debug("get Loge:idx=" + loge.getLogIdx);
                Daos.idxdb.put("R" + loge.getLogIdx, OValue.newBuilder().setExtdata(b).build())
                log.info("set sync Loge:idx=" + loge.getLogIdx + ".........[OK]");
                if (loge.getLogIdx > maxid) {
                  maxid = loge.getLogIdx;
                }
              }
              RSM.instance.updateLastApplidId(maxid);
//            } else {
//              log.warn("cannot get enough entries:wanted:" + startIdx + "-->" + endIdx + ",returnsize=" +
//                ret.getEntriesList.size() + ",after Filter=" + realmap.size);
//            }
          }
        }
        def onFailed(e: java.lang.Exception, fp: FramePacket) {
          log.debug("send SYNRAF ERROR " + n.uri + ",e=" + e.getMessage, e)
        }
      })
    } catch {
      case e: Throwable =>
        log.error("SyncError:" + e.getMessage, e)
    } finally {
      runCounter.decrementAndGet();
    }
  }
}
