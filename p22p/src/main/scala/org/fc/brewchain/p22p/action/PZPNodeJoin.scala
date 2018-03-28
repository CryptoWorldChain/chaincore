package org.fc.brewchain.xdn

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.fc.brewchain.bcapi.crypto.EncHelper
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
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import org.fc.brewchain.p22p.PSMPZP
import org.fc.brewchain.p22p.pbgens.P22P.PCommand
import org.fc.brewchain.p22p.node.NodeInstance
import java.net.URL
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.fc.brewchain.p22p.exception.NodeInfoDuplicated
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.PNode

@NActorProvider
@Slf4j
object PZPNodeJoin extends PSMPZP[PSJoin] {
  override def service = PZPNodeJoinService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PZPNodeJoinService extends OLog with PBUtils with LService[PSJoin] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSJoin, handler: CompleteHandler) = {
    log.debug("onPBPacket::" + pbo)
    var ret = PRetJoin.newBuilder();
    try {
      //       pbo.getMyInfo.getNodeName
      val from = pbo.getMyInfo;
      ret.setMyInfo(toPMNode(NodeInstance.root))
      if (pbo.getOp == PSJoin.Operation.NODE_CONNECT) {
        val _urlcheck = new URL(from.getProtocol + "://" + from.getAddress + ":" + from.getPort)
        if ((from.getTryNodeIdx > 0 && from.getTryNodeIdx == NodeInstance.root().node_idx) ||
          StringUtils.equals(from.getBcuid, NodeInstance.root().bcuid)) {
          log.info("same NodeIdx :" + from.getNodeIdx+",tryIdx="+from.getTryNodeIdx+",bcuid="+from.getBcuid);
          throw new NodeInfoDuplicated("NodeIdx=" + from.getNodeIdx);
        } else if (Networks.instance.node_bits.testBit(from.getTryNodeIdx)) {
          log.info("nodebits duplicated NodeIdx :" + from.getNodeIdx);
          throw new NodeInfoDuplicated("NodeIdx=" + from.getNodeIdx);
        } else {
          //name, idx, protocol, address, port, startup_time, pub_key, counter,idx
          val n = fromPMNode(from);
          log.info("add Pending Node:" + n);
          Networks.instance.addPendingNode(n)
        }
      } else if (pbo.getOp == PSJoin.Operation.NODE_CONNECT) {
        //        NodeInstance.curnode.addPendingNode(new LinkNode(from.getProtocol, from.getNodeName, from.getAddress, // 
        //          from.getPort, from.getStartupTime, from.getPubKey, from.getTryNodeIdx, from.getNodeIdx))
      }

//      ret.addNodes(toPMNode(NodeInstance.root));

      Networks.instance.directNodes.map { _pn =>
        log.debug("directnodes==" + _pn)
        ret.addNodes(toPMNode(_pn));
      }
    } catch {
      case fe: NodeInfoDuplicated => {
        ret.clear();
        ret.addNodes(toPMNode(NodeInstance.root));
        ret.setRetCode(-1).setRetMessage(fe.getMessage)
      }
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
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.JIN.name();
}
