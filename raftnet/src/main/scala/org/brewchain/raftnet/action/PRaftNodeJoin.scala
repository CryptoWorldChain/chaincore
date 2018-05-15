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
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.brewchain.raftnet.pbgens.Raftnet.PRetJoin
import org.brewchain.raftnet.pbgens.Raftnet.PCommand
import org.brewchain.raftnet.tasks.RaftStateManager
import org.brewchain.raftnet.tasks.RSM
import org.brewchain.raftnet.pbgens.Raftnet.RaftState

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PRaftNodeJoin extends PSMRaftNet[PSJoin] {
  override def service = PRaftNodeJoinService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PRaftNodeJoinService extends LogHelper with PBUtils with LService[PSJoin] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSJoin, handler: CompleteHandler) = {
    log.debug("RaftJoinService::" + pack.getFrom())
    var ret = PRetJoin.newBuilder();
    if (!RSM.isReady()) {
      ret.setRetCode(-1).setRetMessage("Raft Network Not READY")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(RSM.raftNet)
        MDCSetMessageID(pbo.getMessageId)
        ret.setMessageId(pbo.getMessageId);
        //
        ret.setRn(RSM.curRN());
        RSM.raftFollowNetByUID.map(rn => {
          ret.addNodes(rn._2);
        })
        //        if (pbo.getRn.getState == RaftState.RS_INIT) {
        RSM.raftFollowNetByUID.put(pbo.getRn.getBcuid, pbo.getRn);
        //        }
        ret.setRetCode(0).setRetMessage("SUCCESS");
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
  override def cmd: String = PCommand.JIN.name();
}
