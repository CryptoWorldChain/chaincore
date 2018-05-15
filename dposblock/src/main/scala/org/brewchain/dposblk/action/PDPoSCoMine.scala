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
import org.brewchain.dposblk.tasks.DCtrl
import onight.tfw.otransio.api.PacketHelper
import org.fc.brewchain.bcapi.exception.FBSException
import org.brewchain.dposblk.pbgens.Dposblock.DNodeState
import org.apache.commons.lang3.StringUtils
import org.brewchain.dposblk.utils.DConfig

@NActorProvider
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PDPoSNodeCoMine extends PSMDPoSNet[PSCoMine] {
  override def service = PDPoSNodeCoMineService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PDPoSNodeCoMineService extends LogHelper with PBUtils with LService[PSCoMine] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSCoMine, handler: CompleteHandler) = {
    log.debug("from::" + pack.getFrom())
    var ret = PRetCoMine.newBuilder();
    if (!DCtrl.isReady() && !StringUtils.equals(pbo.getDn.getCoAddress,
      DCtrl.curDN().getCoAddress)) {
      log.debug("DPoS not ready" + pack.getFrom())
      ret.setRetCode(-1).setRetMessage("DPoS Network Not READY")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(DCtrl.dposNet())
        MDCSetMessageID(pbo.getMessageId)
        ret.setMessageId(pbo.getMessageId);
        //
        //        }
        val cn = DCtrl.curDN()
        ret.setRetCode(0).setRetMessage("SUCCESS").setDn(cn);

        if (pbo.getDn.getCurBlock >= cn.getCurBlock - DConfig.BLOCK_DISTANCE_COMINE) {
          ret.setCoResult(DNodeState.DN_CO_MINER)
          DCtrl.coMinerByUID.map { f =>
            ret.addCoNodes(f._2)
          }
          DCtrl.coMinerByUID.put(pbo.getDn.getBcuid, pbo.getDn)
        } else {
          ret.setCoResult(DNodeState.DN_SYNC_BLOCK)
        }
        log.debug("Join:Success for bcuid=" + pack.getFrom() + ",coresult=" + ret.getCoResult)

      } catch {
        case e: FBSException => {
          log.error("error:", e);

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
  override def cmd: String = PCommand.JIN.name();
}
