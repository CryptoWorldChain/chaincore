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
import org.brewchain.dposblk.utils.DConfig

object BlockSync extends LogHelper {

  val runCounter = new AtomicLong(0);
  def tryBackgroundSyncLogs(block_max_wanted: Int, fastNodeID: String)(implicit network: Network): Unit = {
    Scheduler.runOnce(new Runnable() {
      def run() {
        BlockSync.trySyncBlock(block_max_wanted, fastNodeID);
      }
    })
  }
  def trySyncBlock(block_max_wanted: Int, fastNodeID: String)(implicit network: Network): Unit = {
    val cn = DCtrl.instance.cur_dnode;
    //
    log.debug("try sync block: Max block= " + block_max_wanted + ",cur=" + cn.getCurBlock)
    //request log.
    val pagecount =
      ((block_max_wanted - cn.getCurBlock) / DConfig.SYNCBLK_PAGE_SIZE).asInstanceOf[Int]
    +(if ((block_max_wanted - cn.getCurBlock) % DConfig.SYNCBLK_PAGE_SIZE == 0) 1 else 0)

    //        val cdlcount = Math.min(RConfig.SYNCLOG_MAX_RUNNER, pagecount)
    var cc = cn.getCurBlock + 1;
    while (cc <= block_max_wanted) {
      val runner = DTask_SyncBlock(startIdx = cc, endIdx = 
        Math.min(cc + DConfig.SYNCBLK_PAGE_SIZE - 1, block_max_wanted),
        network = network, fastNodeID, runCounter)
      cc += DConfig.SYNCBLK_PAGE_SIZE
      runCounter.incrementAndGet();
      while (runCounter.get >= DConfig.SYNCBLK_MAX_RUNNER) {
        //wait... for next runner
        try {
          log.debug("waiting for runner:cur=" + runCounter.get)
          this.synchronized(this.wait(DConfig.SYNCBLK_WAITSEC_NEXTRUN))
        } catch {
          case t: InterruptedException =>
          case e: Throwable =>
        }
      }
      Scheduler.runOnce(runner);
    }
    while (runCounter.get > 0) {
      log.debug("waiting for log syncs:" + runCounter.get);
      this.synchronized(Thread.sleep(DConfig.SYNCBLK_WAITSEC_NEXTRUN))
    }
    log.debug("finished init follow up logs:" + DCtrl.curDN().getCurBlock);
    //
  }
}