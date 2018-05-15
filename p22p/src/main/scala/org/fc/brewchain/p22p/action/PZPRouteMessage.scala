package org.fc.brewchain.p22p.action

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
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import org.fc.brewchain.p22p.PSMPZP
import org.fc.brewchain.p22p.pbgens.P22P.PCommand
import java.net.URL
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.exception.NodeInfoDuplicated
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import onight.tfw.mservice.NodeHelper
import com.google.protobuf.Any
import org.fc.brewchain.p22p.pbgens.P22P.PBVoteNodeIdx
import org.fc.brewchain.p22p.pbgens.P22P.PVType
import org.fc.brewchain.p22p.Daos
import org.fc.brewchain.p22p.pbft.StateStorage
import org.fc.brewchain.p22p.pbgens.P22P.PBFTStage
import org.fc.brewchain.p22p.core.MessageSender
import org.brewchain.bcapi.utils.PacketIMHelper._
import org.slf4j.MDC
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.utils.LogHelper
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.bcapi.BCPacket
import org.fc.brewchain.p22p.pbft.VoteQueue
import org.fc.brewchain.p22p.pbft.DMVotingNodeBits
import org.fc.brewchain.p22p.pbft.DMViewChange
import org.fc.brewchain.p22p.pbgens.P22P.PSTestMessage
import org.fc.brewchain.p22p.pbgens.P22P.PRetTestMessage
import org.fc.brewchain.p22p.pbgens.P22P.TestMessageType
import org.fc.brewchain.bcapi.crypto.BitMap
import java.util.concurrent.CountDownLatch
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage
import org.fc.brewchain.p22p.pbgens.P22P.PRetRouteMessage
import org.fc.brewchain.p22p.utils.NodeSetHelper
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNode
import com.google.protobuf.Message
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBRouteMsgType

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor],classOf[CMDService]
) )
class PZPRouteMessage extends PSMPZP[PSRouteMessage] {
  override def service = PZPRouteMessageService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PZPRouteMessageService extends OLog with PBUtils with LService[PSRouteMessage] with PMNodeHelper with LogHelper
    with NodeSetHelper with BitMap {

  var cdl = new CountDownLatch(0)

  override def onPBPacket(pack: FramePacket, pbo: PSRouteMessage, handler: CompleteHandler) = {
    var ret = PRetRouteMessage.newBuilder();

    val network = networkByID(pbo.getNid)
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:" + pbo.getNid)
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {

      MDCSetBCUID(network)

      try {
        val net = networkByID(pbo.getNid);
        val nexthops = pb2scala(pbo.getNextHops);
        val start = System.currentTimeMillis();
        MDCSetMessageID("V|" + pbo.getMessageid);

        val strencbits = net.node_strBits();
        
        val bodybb = Right(pbo.getBody)
        
        log.debug("nexthops=" + nexthops);
        if (strencbits.equals(pbo.getEncbits) && bodybb != null) {
          net.wallMessage(pbo.getGcmd, bodybb , pbo.getMessageid)(nexthops)
        } else {
          log.warn("bit end not equals message gcmd=:" + pbo.getGcmd + ",netenc=" + strencbits
            + ",pboenc=" + pbo.getEncbits + ",body=");
          val bits = mapToBigInt(pbo.getEncbits)
          net.bwallMessage(pbo.getGcmd,  bodybb, bits)
          //                BitMap.hexToMapping(pbo.get))
        }

        //      net.wallMessage(pbo.getGcmd, pbo.getBody, messageid);
        ret.setPendingCount(net.pendingNodes.size)
        ret.setDnodeCount(net.directNodes.size)
        ret.setBitencs(net.node_strBits)
        ret.setRetMessage("TotalCost:" + (System.currentTimeMillis() - start))

        //      }
      } catch {
        case fe: NodeInfoDuplicated => {
          ret.clear();
          ret.setRetCode(-1).setRetMessage("" + fe.getMessage)
        }
        case e: FBSException => {
          ret.clear()
          ret.setRetCode(-2).setRetMessage("" + e.getMessage)
        }
        case t: Throwable => {
          log.error("error:", t);
          ret.clear()
          ret.setRetCode(-3).setRetMessage("" + t.getMessage)
        }
      } finally {
        try {
          handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
        } finally {
          MDCRemoveMessageID
        }
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.RRR.name();
}
