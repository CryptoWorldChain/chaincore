package org.fc.brewchain.p22p.tasks

import java.util.concurrent.TimeUnit
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PBVoteNodeIdx
import java.math.BigInteger
import org.fc.brewchain.p22p.node.PNode
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.core.MessageSender
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CallBack
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import java.net.URL
import org.apache.felix.framework.URLHandlers
import org.fc.brewchain.bcapi.URLHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.fc.brewchain.bcapi.crypto.BitMap
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import org.fc.brewchain.p22p.pbgens.P22P.PBFTStage
import org.fc.brewchain.p22p.node.Networks
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.pbgens.P22P.PSNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PRetNodeInfo

import scala.collection.JavaConversions._
import org.fc.brewchain.p22p.node.Network
import java.util.concurrent.atomic.AtomicBoolean

//投票决定当前的节点
case class CheckingHealthy(network: Network) extends SRunner {
  def getName() = "CheckingHealthy"
  val checking = new AtomicBoolean(false)
  def runOnce() = {
    if (checking.compareAndSet(false, true)) {
      try {
        //    if (!StringUtils.isBlank(network.root().pub_key)) {
        val pack = PSNodeInfo.newBuilder().setNode(toPMNode(network.root()))
          .setNid(network.netid).build()
        implicit val _net = network
        network.pendingNodes.filter { _.bcuid != network.root().bcuid }.map { n =>
          log.debug("checking Health to pending@" + n.bcuid + ",uri=" + n.uri)
          MessageSender.sendMessage("HBTPZP", pack, n, new CallBack[FramePacket] {
            def onSuccess(fp: FramePacket) = {
              log.debug("send HBTPZP success:to " + n.uri + ",body=" + fp.getBody)
              val retpack = PRetNodeInfo.newBuilder().mergeFrom(fp.getBody);
              //          log.debug("get nodes:" + retpack);
              if (retpack.getCurrent == null) {
                log.debug("Node EROR NotFOUND:" + retpack);
                network.removePendingNode(n);
              } else if (!StringUtils.equals(retpack.getCurrent.getBcuid, n.bcuid)) {
                log.debug("Node EROR BCUID Not Equal:" + retpack.getCurrent.getBcuid + ",n=" + n.bcuid);
                network.removePendingNode(n);
              } else {
                log.debug("get nodes:pendingcount=" + retpack.getPnodesCount + ",dnodecount=" + retpack.getDnodesCount);
                network.onlineMap.put(n.bcuid, n);
                def joinFunc(pn: PMNodeInfo) = {
                  val pnode = fromPMNode(pn);
                  network.addPendingNode(pnode);
                  network.joinNetwork.pendingJoinNodes.put(pnode.bcuid, pnode)
                }
                retpack.getPnodesList.map(joinFunc)
                retpack.getDnodesList.map(joinFunc)
              }
            }
            def onFailed(e: java.lang.Exception, fp: FramePacket) {
              log.debug("send HBTPZP ERROR " + n.uri + ",e=" + e.getMessage, e)
              network.removePendingNode(n);
              MessageSender.dropNode(n)
              network.joinNetwork.joinedNodes.remove(n.uri.hashCode());
              network.joinNetwork.pendingJoinNodes.remove(n.bcuid);
            }
          });
        }
        network.directNodes.filter { _.bcuid != network.root().bcuid }.map { n =>

          log.debug("checking Health to directs@" + n.bcuid + ",uri=" + n.uri)
          MessageSender.sendMessage("HBTPZP", pack, n, new CallBack[FramePacket] {
            def onSuccess(fp: FramePacket) = {
              log.debug("send HBTPZP Direct success:to " + n.uri + ",body=" + fp.getBody)
              network.onlineMap.put(n.bcuid, n);
              val retpack = PRetNodeInfo.newBuilder().mergeFrom(fp.getBody);
              log.debug("get nodes:pendingcount=" + retpack.getPnodesCount + ",dnodecount=" + retpack.getDnodesCount);
              retpack.getPnodesList.map { pn =>
                network.addPendingNode(fromPMNode(pn));
              }
              //fix bugs when some node down.2018.3
              retpack.getDnodesList.map { pn =>
                if (network.nodeByBcuid(pn.getBcuid) == network.noneNode) {
                  network.addPendingNode(fromPMNode(pn));
                }
              }
            }
            def onFailed(e: java.lang.Exception, fp: FramePacket) {
              log.debug("send HBTPZP Direct ERROR " + n.uri + ",e=" + e.getMessage, e)
              MessageSender.dropNode(n)
              network.joinNetwork.joinedNodes.remove(n.uri.hashCode());
              network.joinNetwork.pendingJoinNodes.remove(n.bcuid);
              network.removeDNode(n);
            }
          });
        }
      }
      finally{
        checking.compareAndSet(true, false);
      }
    }
    //    }
  }
  //Scheduler.scheduleWithFixedDelay(new Runnable, initialDelay, delay, unit)
  def main(args: Array[String]): Unit = {
    URLHelper.init()
    //System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.bcapi.url");
    println(new URL("tcp://127.0.0.1:5100").getHost);
  }
}