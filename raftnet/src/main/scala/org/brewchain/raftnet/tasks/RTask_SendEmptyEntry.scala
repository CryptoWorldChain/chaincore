package org.brewchain.raftnet.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import onight.tfw.outils.serialize.UUIDGenerator
import org.brewchain.raftnet.pbgens.Raftnet.PSRequestVote
import org.brewchain.raftnet.Daos
import org.brewchain.raftnet.utils.RConfig
import scala.collection.JavaConversions._
import org.fc.brewchain.p22p.core.Votes
import org.fc.brewchain.p22p.core.Votes.Converge
import org.fc.brewchain.p22p.core.Votes.Undecisible
import org.brewchain.raftnet.pbgens.Raftnet.RaftVoteResult
import org.brewchain.raftnet.pbgens.Raftnet.RaftState
import org.brewchain.raftnet.pbgens.Raftnet.PSAppendEntries

//获取其他节点的term和logidx，commitidx
object RTask_SendEmptyEntry extends LogHelper {
  def runOnce(implicit network: Network): Boolean = {
    Thread.currentThread().setName("RTask_SendEmptyEntry");
    val cn = RSM.curRN();
    val msgid = UUIDGenerator.generate();
    log.debug("RTask_SendEmptyEntry.runOnce:msgid="+msgid+",T="+cn.getCurTerm
        +",CI="+cn.getCommitIndex+",N="+cn.getVoteN+",PN="+RSM.raftFollowNetByUID.size)
    val alog = PSAppendEntries.newBuilder()
      .setCommitIdx(cn.getCommitIndex)
      .setLeaderBcuid(cn.getBcuid)
      .setMessageId(msgid)
      .setReqTerm(cn.getCurTerm).build()
    network.wallOutsideMessage("LOGRAF", Left(alog), msgid);
    true
  }

}
