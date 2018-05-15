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
import org.brewchain.dposblk.pbgens.Dposblock.PCommand
import org.brewchain.dposblk.pbgens.Dposblock.PSCoinbase
import org.brewchain.dposblk.pbgens.Dposblock.PRetCoMine
import org.brewchain.dposblk.pbgens.Dposblock.PRetCoinbase
import org.brewchain.dposblk.tasks.DCtrl
import onight.tfw.otransio.api.PacketHelper
import org.brewchain.dposblk.pbgens.Dposblock.PRetCoinbase.CoinbaseResult
import org.fc.brewchain.bcapi.exception.FBSException
import org.apache.commons.lang3.StringUtils
import org.brewchain.dposblk.pbgens.Dposblock.PBlockEntry

@NActorProvider
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PDPoSCoinbaseBlock extends PSMDPoSNet[PSCoinbase] {
  override def service = PDPoSCoinbaseBlockService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PDPoSCoinbaseBlockService extends LogHelper with PBUtils with LService[PSCoinbase] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSCoinbase, handler: CompleteHandler) = {
    log.debug("Mine Block From::" + pack.getFrom())
    var ret = PRetCoinbase.newBuilder();
    if (!DCtrl.isReady()) {
      log.debug("DCtrl not ready");
      ret.setRetCode(-1).setRetMessage("DPoS Network Not READY")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(DCtrl.dposNet())
        MDCSetMessageID(pbo.getMessageId)
        ret.setMessageId(pbo.getMessageId);
        //
        val cn = DCtrl.curDN()
        ret.setRetCode(0).setRetMessage("SUCCESS")
        cn.synchronized {
          if (StringUtils.equals(pbo.getCoAddress, cn.getCoAddress) || pbo.getBlockHeight > cn.getCurBlock) {
            if (DCtrl.checkMiner(pbo.getBlockHeight, pbo.getCoAddress, pbo.getMineTime)) {
              log.debug("Miner is OK:B=" + pbo.getBlockHeight + ",CoAddr=" + pbo.getCoAddress
                + ",T=" + pbo.getTermId + ",CT=" + DCtrl.termMiner().getTermId + ",TU=" + DCtrl.termMiner().getSign
                + ",CB=" + cn.getCurBlock);
              ret.setResult(CoinbaseResult.CR_PROVEN)
              //            if (pbo.getBlockHeight != cn.getCurBlock) {
              DCtrl.saveBlock(PBlockEntry.newBuilder().setBlockHeader(pbo.getBlockHeader.toByteString())
                .setBlockHeight(pbo.getBlockHeight)
                .setCoinbaseBcuid(pbo.getCoAddress)
                .setSign(pbo.getMessageId)
                .setSliceId(pbo.getSliceId))
              if (pbo.getBlockHeight > cn.getCurBlock) {
                cn.setCurBlock(pbo.getBlockHeight)
                DCtrl.instance.syncToDB();
              }

              //            }
            } else {
              log.debug("Miner not for the block:Block=" + pbo.getBlockHeight + ",CA=" + pbo.getCoAddress);
              ret.setResult(CoinbaseResult.CR_REJECT)
            }
          } else {
            log.debug("Current Miner Height is not consequence,PBOH=" + pbo.getBlockHeight + ",CUR=" + cn.getCurBlock);
            ret.setResult(CoinbaseResult.CR_REJECT)
          }
        }

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
  override def cmd: String = PCommand.MIN.name();
}
