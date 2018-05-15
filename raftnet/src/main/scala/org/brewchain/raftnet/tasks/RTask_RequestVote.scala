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
import org.brewchain.raftnet.tasks.RSM;
import org.brewchain.raftnet.pbgens.Raftnet.RaftState

//获取其他节点的term和logidx，commitidx
object RTask_RequestVote extends LogHelper {
  def runOnce(implicit network: Network): Boolean = {
    Thread.currentThread().setName("RTask_RequestVote");
    val cn = RSM.curRN();

    if (RSM.curVR.getReqTerm > cn.getCurTerm &&
      System.currentTimeMillis() - RSM.curVR.getVoteStartMs < RConfig.MAX_VOTE_WAIT_SEC * 1000) {
      // check db
      val records = Daos.raftdb.listBySecondKey("R" + RSM.curVR.getReqTerm)
      log.debug("check db status:T=" + RSM.curVR.getReqTerm + ",N=" + RSM.curVR.getVoteN + ",dbsize=" +
        records.get.size())
      if ((records.get.size() + 1) >= RSM.curVR.getVoteN * RConfig.VOTE_QUORUM_RATIO / 100) {
        log.debug("try to vote:" + records.get.size());
        val reclist = records.get.map { p =>
          PSRequestVote.newBuilder().mergeFrom(p.getValue.getExtdata);
        } ++ Some(RSM.curVR);

        Votes.vote(reclist).PBFTVote({ p =>
          Some(p.getVr)
        }, RSM.curVR.getVoteN) match {
          case Converge(n) =>
            log.debug("converge:" + n);
            if (n == RaftVoteResult.RVR_GRANTED) {
              log.debug("Vote Granted will be the leader:" + n);
              RSM.instance.updateNodeState(RSM.curVR, RaftState.RS_LEADER)
              true
            } else if (n == RaftVoteResult.RVR_NOT_GRANT) {
              log.debug("Vote Not Granted" + n);
              RSM.resetVoteRequest();
              false
            } else {
              log.debug("unknow vote state")
              RSM.resetVoteRequest();
              false
            }
          case n: Undecisible =>

            if (records.get.size() == RSM.curVR.getVoteN - 1) {
              log.debug("Undecisible but not converge.")
              RSM.resetVoteRequest();
            } else {
              log.debug("cannot decide vote state, wait other response")
            }
            false
          case _ =>
            log.debug("not converge,try next time")
            RSM.resetVoteRequest();
            false
        }
      } else {
        false
      }
    } else {

      val newterm = cn.getCurTerm + 1;
      val lastsec = (Math.abs(Math.random() * RConfig.MAX_TERM_SEC) + RConfig.MIN_TERM_SEC).asInstanceOf[Long]
      val msgid = UUIDGenerator.generate();
      MDCSetMessageID(msgid);
      
      log.debug("get RaftNetNodeCount=" + RSM.raftFollowNetByUID.size+",NetworkDNodecount="+network.directNodeByBcuid.size);

      //checking health remove offline nodes.
      RSM.raftFollowNetByUID.filter(p => {
        network.nodeByBcuid(p._1) == network.noneNode
      }).map { p =>
        log.debug("remove Node:" + p._1);
        RSM.raftFollowNetByUID.remove(p._1);
      }
      val curtime = System.currentTimeMillis()
      RSM.curVR = PSRequestVote.newBuilder()
        .setCandidateBcuid(cn.getBcuid)
        .setLastLogIdx(cn.getLastApplied)
        .setLastLogTerm(cn.getCurTerm)
        .setReqTerm(newterm)
        .setTermEndMs(curtime + lastsec * 1000)
        .setMessageId(msgid)
        .setVoteStartMs(curtime)
        .setVoteN(RSM.raftFollowNetByUID.size).build();
      log.debug("try to vote:newterm=" + newterm + ",curterm=" + cn.getCurTerm
        + ",lastsec=" + lastsec + ",voteN=" +
        RSM.curVR.getVoteN)
      network.wallOutsideMessage("VOTRAF", Left(RSM.curVR), msgid);
      false
    }
  }

}
