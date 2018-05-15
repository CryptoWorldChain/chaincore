package org.brewchain.dposblk.action

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService
import onight.osgi.annotation.NActorProvider
import org.brewchain.dposblk.PSMDPoSNet
import org.fc.brewchain.p22p.utils.LogHelper
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.commons.LService
import org.fc.brewchain.p22p.action.PMNodeHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CompleteHandler
import org.brewchain.bcapi.utils.PacketIMHelper._
import org.brewchain.dposblk.pbgens.Dposblock.PSCoMine
import org.brewchain.dposblk.pbgens.Dposblock.PRetCoMine
import org.brewchain.dposblk.pbgens.Dposblock.PCommand
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVote
import org.brewchain.dposblk.pbgens.Dposblock.PDutyTermResult
import org.brewchain.dposblk.tasks.DCtrl
import onight.tfw.otransio.api.PacketHelper
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.bcapi.exception.FBSException
import org.brewchain.dposblk.pbgens.Dposblock.PDutyTermResult.VoteResult
import org.brewchain.dposblk.tasks.BlockSync

@NActorProvider
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PDPoSDutyTermVote extends PSMDPoSNet[PSDutyTermVote] {
  override def service = PDPoSDutyTermVoteService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PDPoSDutyTermVoteService extends LogHelper with PBUtils with LService[PSDutyTermVote] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSDutyTermVote, handler: CompleteHandler) = {
    log.debug("DPoS DutyTermVoteService::" + pack.getFrom())
    var ret = PDutyTermResult.newBuilder();
    val net = DCtrl.instance.network;
    if (!DCtrl.isReady() || net == null) {
      ret.setRetCode(-1).setRetMessage("DPoS Network Not READY")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(DCtrl.dposNet())
        MDCSetMessageID(pbo.getMessageId)
        val cn = DCtrl.curDN()

        ret.setMessageId(pbo.getMessageId);
        ret.setBcuid(cn.getBcuid)
        ret.setRetCode(0).setRetMessage("SUCCESS")
        val vq = DCtrl.voteRequest();
        //
        this.synchronized({
          if ((StringUtils.isBlank(cn.getDutyUid) || cn.getDutyUid.equals(pbo.getLastTermUid))
            //&& (StringUtils.isBlank(vq.getMessageId) || vq.getMessageId.equals(pbo.getLastTermUid))
            && (vq.getTermId <= pbo.getLastTermId) && vq.getTermId < pbo.getTermId 
            || StringUtils.equals(pbo.getCoAddress, cn.getCoAddress)) {
            if (cn.getCurBlock < pbo.getBlockRange.getStartBlock - 1) {
              log.debug("Grant DPos Term Vote but Block Height Not Ready:" + cn.getDutyUid + ",T=" + pbo.getTermId
                + ",VT=" + vq.getTermId + ",LT=" + pbo.getLastTermId
                + ",B=" + cn.getCurBlock + ",BS=[" + pbo.getBlockRange.getStartBlock+"," + pbo.getBlockRange.getEndBlock
                + "],VM=" + vq.getMessageId + ",LTM=" + pbo.getLastTermUid
                + ",PA=" + pbo.getCoAddress + ",CA=" + cn.getCoAddress);
              ret.setResult(VoteResult.VR_GRANTED)
              ret.setTermId(pbo.getTermId)
              ret.setSign(pbo.getSign)
              ret.setVoteAddress(cn.getCoAddress)
              DCtrl.instance.updateVoteReq(pbo);
              BlockSync.tryBackgroundSyncLogs(pbo.getBlockRange.getStartBlock - 1, pbo.getBcuid)(net)
            } else {
              // 
              log.debug("Grant DPos Term Vote:" + cn.getDutyUid + ",T=" + pbo.getTermId
                + ",VT=" + vq.getTermId + ",LT=" + pbo.getLastTermId
                + ",VM=" + vq.getMessageId + ",LTM=" + pbo.getLastTermUid
                + ",PA=" + pbo.getCoAddress + ",CA=" + cn.getCoAddress);
              ret.setResult(VoteResult.VR_GRANTED)
              ret.setTermId(pbo.getTermId)
              ret.setSign(pbo.getSign)
              ret.setVoteAddress(cn.getCoAddress)
              DCtrl.instance.updateVoteReq(pbo);
            }
            //
          } else {
            log.debug("Reject DPos Term Vote:" + cn.getDutyUid + ",T=" + pbo.getTermId
              + ",VT=" + vq.getTermId + ",LT=" + pbo.getLastTermId
              + ",VM=" + vq.getMessageId + ",LTM=" + pbo.getLastTermUid
              + ",PA=" + pbo.getCoAddress + ",CA=" + cn.getCoAddress);
            ret.setResult(VoteResult.VR_REJECT)
            ret.setTermId(pbo.getTermId)
            ret.setSign(pbo.getSign)
            ret.setVoteAddress(cn.getCoAddress)
            //
          }
        })
        net.dwallMessage("DTRDOB", Left(ret.build()), pbo.getMessageId);
        //        }

      } catch {
        case e: FBSException => {
          ret.clear()
          ret.setRetCode(-2).setRetMessage(e.getMessage)
        }
        case t: Throwable => {
          log.error("error:", t);
          ret.clear()
          ret.setRetCode(-3).setRetMessage(t.getMessage)
        }
      } finally {
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.DTV.name();
}
