package org.fc.brewchain.p22p.tasks

import java.util.concurrent.TimeUnit
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PBVoteNodeIdx
import java.math.BigInteger
import scala.collection.JavaConverters
import org.fc.brewchain.p22p.node.PNode
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.core.MessageSender
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CallBack
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import org.fc.brewchain.p22p.Daos
import java.net.URL
import org.fc.brewchain.bcapi.URLHelper
import org.osgi.service.url.URLStreamHandlerService
import onight.tfw.otransio.api.PacketHelper
import scala.collection.mutable.ArrayBuffer
import java.util.HashMap
import org.fc.brewchain.p22p.core.Votes._
import scala.collection.mutable.Map
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.fc.brewchain.p22p.node.Networks
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentHashMap
import org.fc.brewchain.p22p.node.Network
import sun.rmi.log.LogHandler
import org.fc.brewchain.p22p.utils.LogHelper

//投票决定当前的节点
case class JoinNetwork(network: Network, statupNodes: String) extends SRunner with LogHelper {
  def getName() = "JoinNetwork"
  val sameNodes = new HashMap[Integer, PNode]();
  val pendingJoinNodes = new ConcurrentHashMap[String, PNode]();
  val joinedNodes = new HashMap[Integer, PNode]();
  val duplictedInfoNodes = Map[Int, PNode]();
  def runOnce() = {
    Thread.currentThread().setName("JoinNetwork");
    implicit val _net = network
    if (network.inNetwork()) {
      log.debug("CurrentNode In Network");
    } else {
      var hasNewNode = true;

      while (hasNewNode) {
        try {
          hasNewNode = false;
          MDCSetBCUID(network);
          val namedNodes = (statupNodes.split(",").map { x =>
            log.debug("x=" + x)
            PNode.fromURL(x);
          } ++ pendingJoinNodes.values()).filter { x =>
            !sameNodes.containsKey(x.uri.hashCode()) && !joinedNodes.containsKey(x.uri.hashCode()) && //
              !network.isLocalNode(x)
          };
          namedNodes.map { n => //for each know Nodes
            //          val n = namedNodes(0);
            log.debug("JoinNetwork :Run----Try to Join :MainNet=" + n.uri + ",cur=" + network.root.uri);
            if (!network.root.equals(n)) {
              val joinbody = PSJoin.newBuilder().setOp(PSJoin.Operation.NODE_CONNECT).setMyInfo(toPMNode(network.root()))
                .setNid(network.netid)
                .setNodeCount(network.pendingNodeByBcuid.size
                    +network.directNodeByBcuid.size)
                .setNodeNotifiedCount(joinedNodes.size())
                ;
              log.debug("JoinNetwork :Start to Connect---:" + n.uri);
              MessageSender.sendMessage("JINPZP", joinbody.build(), n, new CallBack[FramePacket] {
                def onSuccess(fp: FramePacket) = {
                  log.debug("send JINPZP success:to " + n.uri)
                  val retjoin = PRetJoin.newBuilder().mergeFrom(fp.getBody);
                  if (retjoin.getRetCode() == -1) { //same message
                    log.debug("get Same Node:" + n.getName);
                    sameNodes.put(n.uri.hashCode(), n);
                    //duplictedInfoNodes.+=(n.uri.hashCode() -> n);
                    MessageSender.dropNode(n)
                    val newN = fromPMNode(retjoin.getMyInfo)
                    MessageSender.changeNodeName(n.bcuid, newN.bcuid);
                    network.onlineMap.put(newN.bcuid(), newN)
                    network.addPendingNode(newN);
                  } else if (retjoin.getRetCode() == -2) {
                    log.debug("get duplex NodeIndex:" + n.getName);
                    duplictedInfoNodes.+=(n.uri.hashCode() -> n);

                  } else if (retjoin.getRetCode() == 0) {
                    joinedNodes.put(n.uri.hashCode(), n);
                    val newN = fromPMNode(retjoin.getMyInfo)
                    MessageSender.changeNodeName(n.bcuid, newN.bcuid);
                    network.addPendingNode(newN);
                    retjoin.getNodesList.map { node =>
                      val pnode = fromPMNode(node);
                      if (network.addPendingNode(pnode)) {
                        pendingJoinNodes.put(node.getBcuid, pnode);
                      }
                      //
                    }
                  }
                  log.debug("get nodes:count=" + retjoin.getNodesCount);
                }
                def onFailed(e: java.lang.Exception, fp: FramePacket) {
                  log.debug("send JINPZP ERROR " + n.uri + ",e=" + e.getMessage, e)
                }
              });
            } else {
              log.debug("JoinNetwork :Finished ---- Current node is MainNode");
            }
          }
          if (namedNodes.size == 0) {
            log.debug("cannot reach more nodes. try from begining");
            if (duplictedInfoNodes.size > network.pendingNodes.size / 3 && !network.directNodeByBcuid.contains(network.root().bcuid)) {
              //            val nl = duplictedInfoNodes.values.toSeq.PBFTVote { x => Some(x.node_idx) }
              //            nl.decision match {
              //              case Some(v: BigInteger) =>
              log.debug("duplictedInfoNodes ,change My Index:" + duplictedInfoNodes.size);
              network.removePendingNode(network.root())

              network.changeNodeIdx(duplictedInfoNodes.head._2.node_idx);
              //drop all connection first
              pendingJoinNodes.clear()
              joinedNodes.clear();
              sameNodes.clear();
              //              case _ => {
              //                log.debug("cannot get Converage :" + nl);
              //network.changeNodeIdx();
              //              }
              //            }
              //} else {
              //network.changeNodeIdx();
            }
            //          joinedNodes.clear();
            duplictedInfoNodes.clear();
            //next run try another index;
          } else {
            if (pendingJoinNodes.size() > 0) {
              hasNewNode = true;
            }
          }
        } catch {
          case e: Throwable =>
            log.debug("JoinNetwork :Error", e);
        } finally {
          log.debug("JoinNetwork :[END]")
        }
      }
    }
  }
}