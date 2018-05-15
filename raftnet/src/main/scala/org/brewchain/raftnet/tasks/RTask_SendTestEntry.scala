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
import org.brewchain.raftnet.tasks.LogWriter;
import org.brewchain.raftnet.tasks.RSM;
import org.brewchain.raftnet.pbgens.Raftnet.RaftState
import org.brewchain.raftnet.pbgens.Raftnet.PSAppendEntries
import org.brewchain.raftnet.pbgens.Raftnet.PLogEntry
import com.google.protobuf.ByteString

object RTask_SendTestEntry extends LogHelper {
  def runOnce(implicit network: Network): Boolean = {
    Thread.currentThread().setName("RTask_SendTestEntry");
    val cn = RSM.curRN();
    val msgid = UUIDGenerator.generate();
    log.debug("RTask_SendTestEntry.runOnce:msgid="+msgid+",T="+cn.getCurTerm
        +",CI="+cn.getCommitIndex+",N="+cn.getVoteN+",PN="+RSM.raftFollowNetByUID.size)
    
    val entry = PLogEntry.newBuilder().setFromBcuid(cn.getBcuid).setLogIdx(RSM.instance.getNexLogID())
    .setLogUid(msgid).setTerm(cn.getCurTerm).setData(ByteString.copyFromUtf8("test123"))
    
    val alog = PSAppendEntries.newBuilder()
      .setCommitIdx(cn.getCommitIndex)
      .setLeaderBcuid(cn.getBcuid)
      .setMessageId(msgid)
      .setPrevLogIdx(cn.getLogIdx)
      .addEntries(entry)
      .setReqTerm(cn.getCurTerm).build()
    LogWriter.writeLog(alog, true)
    true
  }

}
