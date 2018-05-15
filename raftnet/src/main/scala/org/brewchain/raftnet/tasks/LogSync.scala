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
import org.brewchain.raftnet.tasks.LogSync;
import org.brewchain.raftnet.tasks.RSM;
import org.brewchain.raftnet.tasks.Scheduler;
import org.brewchain.raftnet.pbgens.Raftnet.PRetSyncEntries

import scala.collection.JavaConversions._
import org.brewchain.raftnet.pbgens.Raftnet.PLogEntry
import org.brewchain.raftnet.Daos
import org.brewchain.bcapi.gens.Oentity.OValue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
object LogSync extends LogHelper {

  val maxWantedCommitIdx = new AtomicLong(0);
  val runCounter = new AtomicLong(0);
  def tryBackgroundSyncLogs(maxCommitIdx: Long, fastNodeID: String)(implicit network: Network): Unit = {
    Scheduler.runOnce(new Runnable() {
      def run() {
        LogSync.trySyncLogs(maxCommitIdx, fastNodeID);
      }
    })
  }
  def trySyncLogs(maxCommitIdx: Long, fastNodeID: String)(implicit network: Network): Unit = {
    val cn = RSM.instance.cur_rnode;
    this.synchronized({
      if (maxWantedCommitIdx.get >= maxCommitIdx || runCounter.get > 0) {
        return ;
      }
    })

    //
    log.debug("get quorum Reply: MaxIdx= " + maxCommitIdx + ",cur=" + cn.getCommitIndex)
    //request log.
    val pagecount =
      ((maxCommitIdx - cn.getCommitIndex) / RConfig.SYNCLOG_PAGE_SIZE).asInstanceOf[Int]
    +(if ((maxCommitIdx - cn.getCommitIndex) % RConfig.SYNCLOG_PAGE_SIZE == 0) 1 else 0)

    //        val cdlcount = Math.min(RConfig.SYNCLOG_MAX_RUNNER, pagecount)
    var cc = cn.getCommitIndex + 1;
    while (cc <= maxCommitIdx) {
      val runner = RTask_SyncLog(startIdx = cc, endIdx = Math.min(cc + RConfig.SYNCLOG_PAGE_SIZE - 1, maxCommitIdx),
        network = network, fastNodeID, runCounter)
      cc += RConfig.SYNCLOG_PAGE_SIZE
      runCounter.incrementAndGet();
      while (runCounter.get >= RConfig.SYNCLOG_MAX_RUNNER) {
        //wait... for next runner
        try {
          log.debug("waiting for runner:cur=" + runCounter.get)
          this.synchronized(this.wait(RConfig.SYNCLOG_WAITSEC_NEXTRUN))
        } catch {
          case t: InterruptedException =>
          case e: Throwable =>
        }
      }
      Scheduler.runOnce(runner);
    }
    while (runCounter.get > 0) {
      log.debug("waiting for log syncs:" + runCounter.get);
      this.synchronized(Thread.sleep(RConfig.SYNCLOG_WAITSEC_NEXTRUN))
    }
    log.debug("finished init follow up logs:" + RSM.curRN().getLastApplied);
    //
  }
}