package org.brewchain.raftnet.action

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.LService
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.bcapi.exception.FBSException
import org.apache.commons.lang3.StringUtils
import java.util.HashSet
import onight.tfw.outils.serialize.UUIDGenerator
import scala.collection.JavaConversions._
import org.apache.commons.codec.binary.Base64
import java.net.URL
import org.brewchain.bcapi.utils.PacketIMHelper._
import org.brewchain.raftnet.pbgens.Raftnet.PSJoin
import org.brewchain.raftnet.PSMRaftNet
import org.brewchain.raftnet.action.PRaftAppendEntriesService;
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.brewchain.raftnet.pbgens.Raftnet.PRetJoin
import org.brewchain.raftnet.pbgens.Raftnet.PCommand
import org.brewchain.raftnet.pbgens.Raftnet.PSRequestVote
import org.brewchain.raftnet.pbgens.Raftnet.PRetRequestVote
import org.brewchain.raftnet.pbgens.Raftnet.PSAppendEntries
import org.brewchain.raftnet.pbgens.Raftnet.PRetAppendEntries
import org.brewchain.raftnet.tasks.RSM
import org.brewchain.raftnet.pbgens.Raftnet.RaftState
import org.brewchain.raftnet.tasks.RTask_LogWriter
import org.brewchain.raftnet.tasks.LogWriter
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PRaftAppendEntries extends PSMRaftNet[PSAppendEntries] {
  override def service = PRaftAppendEntriesService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PRaftAppendEntriesService extends LogHelper with PBUtils with LService[PSAppendEntries] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSAppendEntries, handler: CompleteHandler) = {
    log.debug("add entry::" + pack.getFrom())
    var ret = PRetAppendEntries.newBuilder();
    val network = networkByID("raft")
    if (network == null) {
      //      ret.setRetCode(-1).setRetMessage("unknow network:Raft")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(network)
        MDCSetMessageID(pbo.getMessageId)
        val cn = RSM.curRN();
        if (pbo.getReqTerm == cn.getCurTerm && !StringUtils.equals(pbo.getLeaderBcuid, cn.getBcuid)) {
          cn.getState match {
            case RaftState.RS_CANDIDATE =>
              log.debug("get leader message:");
              RSM.instance.updateNodeState(RaftState.RS_FOLLOWER, pbo.getReqTerm, pbo.getLeaderBcuid)
            case RaftState.RS_LEADER =>
              if (pbo.getReqTerm > cn.getCurTerm) {
                log.warn("get duplex leader message: upgrade to follow");
                RSM.instance.updateNodeState(RaftState.RS_FOLLOWER, pbo.getReqTerm, pbo.getLeaderBcuid)
              } else {
                log.info(" Term Equal but Current State is Leader: ERROR!");
              }
            case _ =>
              log.debug("get leader message ok")
          }
        }
        if (StringUtils.equals(pbo.getLeaderBcuid, cn.getVotedFor)) {
          cn.getState match {
            case RaftState.RS_LEADER =>
              if (pbo.getReqTerm.equals(cn.getCurTerm) && StringUtils.equals(pbo.getLeaderBcuid, cn.getBcuid)) {
                LogWriter.writeLog(pbo, true)
              } else {
                log.info("Leader Term is lower than me REJECT!Remote=" + pbo.getReqTerm + ",Local=" + cn.getCurTerm);
              }
            case RaftState.RS_CANDIDATE | RaftState.RS_FOLLOWER =>
              if (StringUtils.isBlank(pbo.getLeaderBcuid)) {
                log.debug("route to Leader:" + cn.getVotedFor + ",leaderbcuid=" + pbo.getLeaderBcuid);
                if (StringUtils.isNotBlank(cn.getVotedFor)) {
                  RSM.raftNet().postMessage("LOGRAF", Left(pbo), pbo.getMessageId, cn.getVotedFor)
                }
              } else {
                if (cn.getState == RaftState.RS_CANDIDATE) {
                  log.debug("update current state message ok")
                  RSM.instance.updateNodeState(RaftState.RS_FOLLOWER, pbo.getReqTerm, pbo.getLeaderBcuid)
                }
                LogWriter.writeLog(pbo, false)
              }
            case _ =>
              log.debug("current state cannot append:" + cn.getState)
          }
        } else {
          log.debug("Not in the Same RaftNet Req=" + pbo.getLeaderBcuid + ",MyVote=" + cn.getVotedFor + ",State=" + cn.getState)
        }

      } catch {
        case e: FBSException => {
          ret.clear()
        }
        case t: Throwable => {
          log.error("error:", t);
          ret.clear()
        }
      } finally {
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.LOG.name();
}
