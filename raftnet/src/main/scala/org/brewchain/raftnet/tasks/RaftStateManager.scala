package org.brewchain.raftnet.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import org.brewchain.raftnet.pbgens.Raftnet.RaftState
import org.brewchain.raftnet.tasks.LogSync;
import org.brewchain.raftnet.tasks.RSM;
import org.brewchain.raftnet.tasks.RTask_Join;
import org.brewchain.raftnet.tasks.RTask_RequestVote;
import org.brewchain.raftnet.tasks.RTask_SendEmptyEntry;
import org.brewchain.raftnet.tasks.RTask_SendTestEntry;
import org.brewchain.raftnet.Daos
import org.brewchain.raftnet.pbgens.Raftnet.PRaftNode
import org.brewchain.raftnet.pbgens.Raftnet.PRaftNodeOrBuilder
import org.brewchain.bcapi.gens.Oentity.OValue
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.node.Node
import org.brewchain.raftnet.utils.RConfig
import scala.collection.mutable.Map
import org.brewchain.raftnet.pbgens.Raftnet.PSRequestVote
import java.util.concurrent.atomic.AtomicLong
import org.fc.brewchain.bcapi.JodaTimeHelper

//投票决定当前的节点
case class RaftStateManager(network: Network) extends SRunner with LogHelper {
  def getName() = "RSM"
  val RAFT_NODE_DB_KEY = "CURRENT_RAFT_KEY";
  var cur_rnode: PRaftNode.Builder = PRaftNode.newBuilder()
  var imPRnode: PRaftNode = cur_rnode.build()

  val logIdxCounter = new AtomicLong(1)

  def getNexLogID(): Long = {
    logIdxCounter.incrementAndGet()
  }

  def retsetLogID(idx: Long): Unit = {
    logIdxCounter.set(idx)
  }

  def updateLastApplidId(lastApplied: Long): Boolean = {
    this.synchronized({
      if (lastApplied > cur_rnode.getCommitIndex) {
        log.debug("updateLastApplidId:Cur=" + cur_rnode.getCommitIndex + ",A=" + cur_rnode.getLastApplied
          + ",new=" + lastApplied)
        cur_rnode.setLastApplied(lastApplied).setLogIdx(lastApplied).setCommitIndex(lastApplied)
        imPRnode = cur_rnode.build();
        if (lastApplied - cur_rnode.getCommitIndex > RConfig.COMMIT_LOG_BATCH
          || System.currentTimeMillis() - cur_rnode.getLastCommitTime > RConfig.COMMIT_LOG_TIMEOUT_SEC * 1000) {
          syncCurnodToDB();
        }
        true
      } else {
        false
      }
    })
  }

  def updateNodeState(newState: RaftState, term: Long = -1, voteFor: String = null): Unit = {
    this.synchronized({
      cur_rnode.setState(newState);
      if (term > 0 && term > cur_rnode.getCurTerm) {
        cur_rnode.setCurTerm(term);
      }
      if (voteFor != null) {
        cur_rnode.setVotedFor(voteFor)
      } else {
        cur_rnode.clearVotedFor().clearTermUid()
      }
      imPRnode = cur_rnode.build();
    })
  }

  def updateNodeState(vr: PSRequestVote, newState: RaftState): Boolean = {
    this.synchronized({
      if (vr.getReqTerm > cur_rnode.getCurTerm && cur_rnode.getState != RaftState.RS_LEADER &&
        (vr.getTermEndMs - System.currentTimeMillis()) / 1000 < RConfig.MAX_TERM_SEC) {
        cur_rnode.setState(newState)
          .setTermEndMs(vr.getTermEndMs)
          .setVoteN(vr.getVoteN)
          .setVotedFor(vr.getCandidateBcuid)
          .setTermUid(vr.getMessageId)
          .setCurTerm(vr.getReqTerm)
        imPRnode = cur_rnode.build();
        this.syncCurnodToDB()
        true
      } else {
        false
      }

    })
  }

  def updateNodeIdxs(leader: PRaftNodeOrBuilder): Unit = {
    this.synchronized({
      if (leader.getCurTerm > cur_rnode.getCurTerm) {
        cur_rnode.setCurTerm(leader.getCurTerm);
        cur_rnode.setTermStartMs(leader.getTermStartMs)
        cur_rnode.setTermEndMs(leader.getTermEndMs)
          .setVoteN(leader.getVoteN)
          .setTermUid(leader.getTermUid)
        cur_rnode.setState(RaftState.RS_FOLLOWER);
      } else if (leader == cur_rnode) {
        cur_rnode.setState(RaftState.RS_FOLLOWER);
      }
      imPRnode = cur_rnode.build();
    })
    if (leader.getCommitIndex > cur_rnode.getCommitIndex) {
      LogSync.tryBackgroundSyncLogs(leader.getCommitIndex, leader.getBcuid)(network);
    }
  }
  def loadNodeFromDB(): PRaftNode.Builder = {
    val ov = Daos.raftdb.get(RAFT_NODE_DB_KEY).get
    val root_node = network.root();
    if (ov == null) {
      cur_rnode.setAddress(root_node.v_address).setBcuid(root_node.bcuid)
        .setLogIdx(1)
      Daos.raftdb.put(RAFT_NODE_DB_KEY,
        OValue.newBuilder().setExtdata(cur_rnode.build().toByteString()).build())
    } else {
      cur_rnode.mergeFrom(ov.getExtdata)
      if (!StringUtils.equals(cur_rnode.getAddress, root_node.v_address)
        || !StringUtils.equals(cur_rnode.getBcuid, root_node.bcuid)) {
        log.warn("load from raftnode info not equals with pzp node:" + cur_rnode + ",root=" + root_node)
      } else {
        log.info("load from db:OK" + cur_rnode)
      }
    }
    imPRnode = cur_rnode.build();
    cur_rnode
  }
  def syncCurnodToDB() {
    Daos.raftdb.put(RAFT_NODE_DB_KEY,
      OValue.newBuilder().setExtdata(cur_rnode.build().toByteString()).build())
  }
  def runOnce() = {
    Thread.currentThread().setName("RaftStateManager");
    implicit val _net = network
    MDCSetBCUID(network);
    MDCRemoveMessageID()
    try {
      //      RaftStateManager.rsm = this;
      log.info("RSM.RunOnce:S=" + cur_rnode.getState + ",T=" + cur_rnode.getCurTerm + ",L=" + cur_rnode.getLogIdx
        + ",N=" + cur_rnode.getVoteN
        + ",RN=" + RSM.raftFollowNetByUID.size
        + ",OL=" + cur_rnode.getLastApplied + ",CL=" + cur_rnode.getCommitIndex
        + ",NextSec=" + JodaTimeHelper.secondFromNow(cur_rnode.getTermEndMs)
        + ",Leader=" + cur_rnode.getVotedFor + ",TUID=" + cur_rnode.getTermUid
        + ",VR.T=" + RSM.curVR.getReqTerm
        + ",VR.P=" + JodaTimeHelper.secondFromNow(RSM.curVR.getVoteStartMs));
      cur_rnode.getState match {
        case RaftState.RS_INIT =>
          //tell other I will join
          loadNodeFromDB();
          RSM.raftFollowNetByUID.put(RSM.curRN().getBcuid, RSM.curRN());

          RTask_Join.runOnce match {
            case n: PRaftNodeOrBuilder =>
              updateNodeIdxs(n);
            case x @ _ =>
              log.debug("not other nodes :" + x)
          }
        case RaftState.RS_FOLLOWER =>
          //time out to elect candidate
          //          if(network.directNodes.size > cur_rnode.getVoteN){
          //            //has other node coming
          //            RTask_Join.runOnce
          //          }else
          if (System.currentTimeMillis() > cur_rnode.getTermEndMs) {
            val sleeptime =
              Math.abs((Math.random() * RConfig.CANDIDATE_MAX_WAITMS) +
                RConfig.CANDIDATE_MIN_WAITMS).asInstanceOf[Long]
            log.debug("follow sleep to be candidate:" + sleeptime);
            updateNodeState(RaftState.RS_CANDIDATE);
            Thread.sleep(sleeptime)
          } else {
            RTask_Join.runOnce
          }
        case RaftState.RS_CANDIDATE =>
          //check vote result
          if (System.currentTimeMillis() > cur_rnode.getTermEndMs) {
            //elected
            //try to elect
            if (RTask_RequestVote.runOnce) {
              // i will be master
              //wall logs//send log immediately
              retsetLogID(cur_rnode.getLogIdx);
              RTask_SendEmptyEntry.runOnce
            }
          }
        case RaftState.RS_LEADER =>
          //time out to become follower
          //
          if (System.currentTimeMillis() > cur_rnode.getTermEndMs) {
            //elected
            //try to elect
            updateNodeState(RaftState.RS_FOLLOWER)
          } else {
            RTask_SendTestEntry.runOnce
          }
        case _ =>
          log.warn("unknow State:" + cur_rnode.getState);

      }

    } catch {
      case e: Throwable =>
        log.debug("raft sate managr :Error", e);
    } finally {
      MDCRemoveMessageID()
    }
  }
}

object RSM {
  var instance: RaftStateManager = RaftStateManager(null);
  def raftNet(): Network = instance.network;
  def curRN(): PRaftNode = instance.imPRnode
  var curVR: PSRequestVote = PSRequestVote.newBuilder().build();
  val raftFollowNetByUID: Map[String, PRaftNode] = Map.empty[String, PRaftNode];
  def resetVoteRequest() {
    curVR = PSRequestVote.newBuilder().build();
    //    instance.cur_rnode.setTermEndMs(System.currentTimeMillis() + Math.abs((Math.random() * RConfig.CANDIDATE_MAX_WAITMS) +
    //      RConfig.CANDIDATE_MIN_WAITMS).asInstanceOf[Long])
  }
  def isReady(): Boolean = {
    instance.network != null &&
      instance.cur_rnode.getStateValue > RaftState.RS_INIT_VALUE
  }

}