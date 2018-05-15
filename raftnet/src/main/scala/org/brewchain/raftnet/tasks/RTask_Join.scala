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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.brewchain.raftnet.pbgens.Raftnet.PRaftNodeOrBuilder
import org.fc.brewchain.bcapi.crypto.BitMap

//获取其他节点的term和logidx，commitidx
object RTask_Join extends LogHelper with BitMap {
  def runOnce(implicit network: Network): PRaftNodeOrBuilder = {
    Thread.currentThread().setName("RTask_Join");
    val join = PSJoin.newBuilder().setRn(RSM.curRN()).build();
    val msgid = UUIDGenerator.generate();
    val cn = RSM.instance.cur_rnode;
    var fastNode: PRaftNodeOrBuilder = cn;
    var minCost: Long = Long.MaxValue;
    var maxCommitIdx: Long = 0;
    MDCSetBCUID(network)

    network.directNodes.filter { n => !RSM.raftFollowNetByUID.contains(n.bcuid) }.map { n =>
      val start = System.currentTimeMillis();
      network.sendMessage("JINRAF", join, n, new CallBack[FramePacket] {
        def onSuccess(fp: FramePacket) = {
          log.debug("send JINRAF success:to " + n.uri + ",body=" + fp.getBody)
          val end = System.currentTimeMillis();
          val retjoin = PRetJoin.newBuilder().mergeFrom(fp.getBody);
          if (retjoin.getRetCode() == 0) { //same message
            if (fastNode == null) {
              fastNode = retjoin.getRn;
            } else if (retjoin.getRn.getCurTerm >= fastNode.getCurTerm && retjoin.getRn.getCommitIndex >= maxCommitIdx) {
              if (end - start < minCost) { //set the fast node
                minCost = end - start
                fastNode = retjoin.getRn;
              }
            }
            log.debug("get other nodeInfo:T=" + retjoin.getRn.getCurTerm +
              ",commitLog=" + retjoin.getRn.getCommitIndex + ",lastapply=" +
              retjoin.getRn.getLastApplied);
            RSM.raftFollowNetByUID.put(retjoin.getRn.getBcuid, retjoin.getRn);
          }
        }
        def onFailed(e: java.lang.Exception, fp: FramePacket) {
          log.debug("send JINPZP ERROR " + n.uri + ",e=" + e.getMessage, e)
        }
      })
    }
    log.debug("get nodes:count=" + RSM.raftFollowNetByUID.size+",raftnetNodecount="+network.directNodeByBcuid.size);
    //remove off line
    RSM.raftFollowNetByUID.filter(p => {
      network.nodeByBcuid(p._1) == network.noneNode
    }).map { p =>
      log.debug("remove Node:" + p._1);
      RSM.raftFollowNetByUID.remove(p._1);
    }

    val (maxterm, maxapply, maxcommitIdx) = RSM.raftFollowNetByUID.values.foldLeft((0L, 0L, 0L))((A, n) =>
      (Math.max(A._1, n.getCurTerm), Math.max(A._2, n.getLastApplied), Math.max(A._3, n.getCommitIndex)))
    log.debug("Get Max(T,A,C):" + maxterm + "," + maxapply + "," + maxcommitIdx
      + "::Cur(T,A,C):" + cn.getCurTerm + "," + cn.getLastApplied + "," + cn.getCommitIndex);

    if (fastNode != null && RSM.raftFollowNetByUID.size >= network.directNodes.size * RConfig.VOTE_QUORUM_RATIO / 100
      && cn.getCommitIndex < maxCommitIdx) {
      if (fastNode.getCurTerm != maxterm) {
        log.warn("Raft Net May be Error:node max term not in max commitIdx:");
      }
      LogSync.trySyncLogs(maxCommitIdx, fastNode.getBcuid);
      fastNode
    } else if (cn.getCommitIndex >= maxCommitIdx) {
      log.debug("ready to become follow:cn.getCommitIndex is max:cur=" + cn.getCommitIndex + ", net=" + maxCommitIdx);
      cn
    } else {
      null
    }

  }

}
