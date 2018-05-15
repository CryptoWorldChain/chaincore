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
import org.brewchain.raftnet.action.PRaftRequestVoteService;
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.brewchain.raftnet.pbgens.Raftnet.PRetJoin
import org.brewchain.raftnet.pbgens.Raftnet.PCommand
import org.brewchain.raftnet.pbgens.Raftnet.PSRequestVote
import org.brewchain.raftnet.pbgens.Raftnet.PRetRequestVote
import org.brewchain.raftnet.tasks.RSM
import org.brewchain.raftnet.Daos
import org.brewchain.bcapi.gens.Oentity.OValue
import org.brewchain.raftnet.pbgens.Raftnet.RaftState
import org.brewchain.raftnet.pbgens.Raftnet.RaftVoteResult
import org.fc.brewchain.bcapi.JodaTimeHelper

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService
import org.brewchain.raftnet.tasks.LogSync

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PRaftRequestVote extends PSMRaftNet[PSRequestVote] {
  override def service = PRaftRequestVoteService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PRaftRequestVoteService extends LogHelper with PBUtils with LService[PSRequestVote] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSRequestVote, handler: CompleteHandler) = {
    log.debug("RequestVoteService::" + pack.getFrom())
    var ret = PRetRequestVote.newBuilder();
    val network = networkByID("raft")
    if (network == null) {
      //      ret.setRetCode(-1).setRetMessage("unknow network:Raft")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(network)
        MDCSetMessageID(pbo.getMessageId)
        if (RSM.isReady()) {
          if (StringUtils.isNotBlank(pbo.getResultFrom)) {
            if (RSM.curRN().getState == RaftState.RS_CANDIDATE && StringUtils.equals(pbo.getMessageId, RSM.curVR.getMessageId) &&
              pbo.getReqTerm == RSM.curVR.getReqTerm &&
              pbo.getCandidateBcuid == RSM.curVR.getCandidateBcuid) {
              log.debug("get VoteResults from:" + pbo.getResultFrom+",result="+pbo.getVr);
              Daos.raftdb.put(pbo.getResultFrom + "-" + pbo.getMessageId + "-" + pbo.getReqTerm, // 
                OValue.newBuilder().setSecondKey("R" + pbo.getReqTerm)
                  .setExtdata(pbo.toByteString())
                  .build())
            } else {
              log.debug("cannot put vote result:Cur(S=" + RSM.curRN().getState + ",N=" + RSM.curVR.getCandidateBcuid
                + ",T=" + RSM.curVR.getReqTerm + "),PBO(ReqTerm=" + pbo.getReqTerm + ",N=" + pbo.getCandidateBcuid + ")")
            }
          } else {
            log.debug("get requestvote:T=" + pbo.getReqTerm + ",curT=" + RSM.curRN().getCurTerm)
            val newv = pbo.toBuilder();
            newv.setResultFrom(RSM.curRN.getBcuid)
            if (RSM.instance.updateNodeState(pbo, RaftState.RS_FOLLOWER)) {
              //newv.setVoteN(value)
              newv.setVr(RaftVoteResult.RVR_GRANTED)
              if(pbo.getLastLogIdx > RSM.curRN().getLastApplied){
                //sync log
                LogSync.tryBackgroundSyncLogs(pbo.getLastLogIdx,pbo.getCandidateBcuid)(network);
              }
                  
              log.debug("grant leader for vote:B=" + newv.getCandidateBcuid + ",N=" + newv.getVoteN + ",T="
                + newv.getReqTerm + ",NextSec="
                + JodaTimeHelper.secondFromNow(pbo.getTermEndMs));
              RSM.resetVoteRequest()
            } else {
              newv.setVr(RaftVoteResult.RVR_NOT_GRANT)
            }
            RSM.raftNet().postMessage("VOTRAF", Left(newv.build()), newv.getMessageId, newv.getCandidateBcuid);
          }
        } else {
          log.debug("RSM not ready cannot vote!");
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
  override def cmd: String = PCommand.VOT.name();
}
