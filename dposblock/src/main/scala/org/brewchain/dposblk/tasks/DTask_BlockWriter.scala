package org.brewchain.dposblk.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import onight.tfw.outils.serialize.UUIDGenerator
import onight.tfw.async.CallBack
import onight.tfw.otransio.api.beans.FramePacket

import scala.collection.JavaConversions._
import org.brewchain.bcapi.gens.Oentity.OValue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import org.brewchain.bcapi.gens.Oentity.OKey
import com.google.protobuf.ByteString
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.StringUtils
import org.brewchain.dposblk.pbgens.Dposblock.PRetSyncBlocks
import org.brewchain.dposblk.Daos

//获取其他节点的term和logidx，commitidx
case class DTask_BlockWriter(pbo: PRetSyncBlocks,
    runCounter: AtomicLong, wall: Boolean = false) extends SRunner with LogHelper {
  def getName(): String = "LogW:"

  def runOnce() = {
    //
    try {
      val keys = ArrayBuffer.empty[OKey]
      val values = ArrayBuffer.empty[OValue]
      var maxAppledid = 0L;

      val cn = DCtrl.curDN();
      val kvs = pbo.getBlockHeadersList.map { e =>
        keys.add(OKey.newBuilder().setData(ByteString.copyFromUtf8("D" + e.getBlockHeight)).build())
        values.add(OValue.newBuilder().setExtdata(e.getBlockHeader)
          .setCount(e.getBlockHeight).setInfo(e.getCoinbaseBcuid + "," + e.getSign)
          .build())
      }
      Daos.dposdb.batchPuts(keys.toArray, values.toArray)
//        !!RSM.instance.updateLastApplidId(maxAppledid)

      if (wall) {
        //!!DCtrl.raftNet().wallOutsideMessage("LOGRAF", Left(pbo), pbo.getMessageId)
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
  def writeLog(pbo: PRetSyncBlocks, wall: Boolean): Unit = {
    log.debug("write Log:CC=" + pbo.getBlockHeadersCount)
    if (pbo.getBlockHeadersCount > 0) {
      val lr = DTask_BlockWriter(pbo, runCounter, wall)
      Scheduler.runOnce(lr)
    }
    //sync
  }

}
