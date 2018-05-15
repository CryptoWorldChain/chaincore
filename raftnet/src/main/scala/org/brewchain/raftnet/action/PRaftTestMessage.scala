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
import java.util.concurrent.ConcurrentHashMap
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.brewchain.raftnet.PSMRaftNet
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PRaftTestMessage extends PSMRaftNet[PSTestMessage] {
  override def service = new PRaftTestMessageService()
}

// http://localhost:8000/fbs/xdn/pbget.do?bd=
class PRaftTestMessageService extends OLog with PBUtils with LService[PSTestMessage] with PMNodeHelper with LogHelper {

  val cdlMap = new ConcurrentHashMap[String, CountDownLatch]; //new CountDownLatch(0)

  override def onPBPacket(pack: FramePacket, pbo: PSTestMessage, handler: CompleteHandler) = {
    val messageid =
      if (StringUtils.isBlank(pbo.getMessageid)) {
        UUIDGenerator.generate();
      } else {
        pbo.getMessageid
      }
    var ret = PRetTestMessage.newBuilder();
    implicit val network = networkByID(pbo.getNid)
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:" + pbo.getNid)
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      //      MDCSetBCUID(network)
      //      MDCSetMessageID(pbo.getTypeValue + "|" + messageid);
      val cdl = if (cdlMap.containsKey(messageid)) cdlMap.get(messageid) else {
        new CountDownLatch(0);
      }
      log.debug("TestMessage:Type=" + pbo.getType)

      try {
        pbo.getType match {
          case TestMessageType.WALL =>
            log.debug("Get Wall Message:COST:" + (System.currentTimeMillis() - pbo.getWallTime));
            if (cdl.getCount > 0 && StringUtils.isNotBlank(pbo.getContent)) {
              ret.setRetCode(-2).setRetMessage("waiting for last call:");
            } else {

              //              val cdlr = new CountDownLatch(network.directNodes.size +
              //                network.pendingNodes.size)
              //              cdlMap.put(messageid, cdlr);
              val start = System.currentTimeMillis();
              val msg = Left(pbo.toBuilder()
                .setMessageid(messageid).setWallTime(System.currentTimeMillis())
                .setFromBcuid(network.root().bcuid)
                .setType(TestMessageType.PING)
                .setOrgBcuid(network.root().bcuid)
                .setNid(pbo.getNid)
                .build());
              if (pbo.getDwall) {
                network.dwallMessage("TTTPZP", msg, messageid);
              } else {
                network.wallMessage("TTTPZP", msg, messageid);
              }
              if (pbo.getBlock) {

                Thread.currentThread().synchronized {
                  try {
                    //                    cdlr.await()
                  } catch {
                    case _: Throwable =>
                  }
                }
              }
              //              cdlMap.remove(messageid)

              //              ret.setDnodeCount(network.directNodes.size)
              //              ret.setPendingCount(network.pendingNodes.size)
              ret.setBitencs(network.node_strBits)
              ret.setRetMessage("TotalCost:" + (System.currentTimeMillis() - start))
              log.debug("get Ret:" + (System.currentTimeMillis() - start))
            }
          case TestMessageType.PING =>
            log.debug("Get Ping Message:COST:" + (System.currentTimeMillis() - pbo.getWallTime));
            Thread.sleep(pbo.getPs + 1)
            MessageSender.postMessage("TTTPZP", Left(pbo.toBuilder()
              .setRecvTime(System.currentTimeMillis())
              .setType(TestMessageType.PONG)
              .setNid(pbo.getNid)
              .setFromBcuid(network.root().bcuid)
              .build()),
              network.nodeByBcuid(pbo.getOrgBcuid));

          case TestMessageType.PONG =>
            if (cdl != null) {
              cdl.countDown()
            }
            log.debug("Get Pong Message:COST:" + (System.currentTimeMillis() - pbo.getRecvTime)
              + ",TOTAL:" + (System.currentTimeMillis() - pbo.getWallTime));
          case _ =>
            ret.setRetCode(-1).setRetMessage("UNKNOW TYPE");
        }

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
          //          MDCRemoveMessageID
        }
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.TTT.name();
}
