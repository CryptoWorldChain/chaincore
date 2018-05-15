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
import org.fc.brewchain.bcapi.crypto.BitMap
import org.brewchain.raftnet.PSMRaftNet
import org.brewchain.raftnet.pbgens.Raftnet.PSNodeInfo
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.fc.brewchain.p22p.pbgens.P22P.PRetNodeInfo
import org.brewchain.raftnet.pbgens.Raftnet.PCommand

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PRaftNodeInfo extends PSMRaftNet[PSNodeInfo] {
  override def service = PRaftNodeInfoService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PRaftNodeInfoService extends LogHelper with PBUtils with LService[PSNodeInfo] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSNodeInfo, handler: CompleteHandler) = {
    log.debug("onPBPacket::" + pbo)
    var ret = PRetNodeInfo.newBuilder();
    val network = networkByID("raft")
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:")
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(network);
        //       pbo.getMyInfo.getNodeName
        ret.setCurrent(toPMNode(network.root()))
        val pending = network.pendingNodes;
        val directNodes = network.directNodes;
        log.debug("pending=" + network.pendingNodes.size + "::" + network.pendingNodes)
        //      ret.addNodes(toPMNode(NodeInstance.curnode));
        pending.map { _pn =>
          log.debug("pending==" + _pn)
          ret.addPnodes(toPMNode(_pn));
        }
        directNodes.map { _pn =>
          log.debug("directnodes==" + _pn)
          ret.addDnodes(toPMNode(_pn));
        }
        ret.setBitEncs(network.node_strBits);
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
  override def cmd: String = PCommand.INF.name();
}
