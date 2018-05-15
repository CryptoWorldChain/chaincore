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
import org.brewchain.raftnet.pbgens.Raftnet.PRetSyncEntries

import scala.collection.JavaConversions._
import org.brewchain.raftnet.pbgens.Raftnet.PLogEntry
import org.brewchain.raftnet.Daos
import org.brewchain.bcapi.gens.Oentity.OValue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import org.brewchain.bcapi.gens.Oentity.OKey
import com.google.protobuf.ByteString
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import org.brewchain.raftnet.pbgens.Raftnet.PSAppendEntries
import org.apache.commons.lang3.StringUtils

//获取其他节点的term和logidx，commitidx
case class RTask_LogWriter(pbo: PSAppendEntries,
    runCounter: AtomicLong, wall: Boolean = false) extends SRunner with LogHelper {
  def getName(): String = "LogW:"

  def runOnce() = {
    //
    try {
      val keys = ArrayBuffer.empty[OKey]
      val values = ArrayBuffer.empty[OValue]
      var maxAppledid = 0L;

      val cn = RSM.curRN();
      val kvs = pbo.getEntriesList.map { e =>
        val (logidx, loguid, term) = if (StringUtils.isBlank(e.getLogUid) || e.getLogIdx == 0) {
          (RSM.instance.getNexLogID(), UUIDGenerator.generate(), cn.getCurTerm)
        } else {
          (e.getLogIdx, e.getLogUid, e.getTerm)
        }
        keys.add(OKey.newBuilder().setData(ByteString.copyFromUtf8("R" + logidx)).build())
        values.add(OValue.newBuilder().setExtdata(e.getData)
          .setCount(logidx).setInfo(loguid + "," + e.getSign)
          .setNonce(term.asInstanceOf[Int])
          .build())
        if (e.getLogIdx > maxAppledid) {
          maxAppledid = e.getLogIdx;
        }
      }
      Daos.idxdb.batchPuts(keys.toArray, values.toArray)
        RSM.instance.updateLastApplidId(maxAppledid)

      if (wall) {
        RSM.raftNet().wallOutsideMessage("LOGRAF", Left(pbo), pbo.getMessageId)
      }
    } catch {
      case e: Throwable =>
        log.error("SyncError:" + e.getMessage, e)
    } finally {
      runCounter.decrementAndGet();
    }
  }

}
object LogWriter extends LogHelper {
  val runCounter = new AtomicLong(0)
  def writeLog(pbo: PSAppendEntries, wall: Boolean): Unit = {
    log.debug("write Log:CC=" + pbo.getEntriesCount)
    if (pbo.getEntriesCount > 0) {
      val lr = RTask_LogWriter(pbo, runCounter, wall)
      Scheduler.runOnce(lr)
    }
    //sync
  }

}
