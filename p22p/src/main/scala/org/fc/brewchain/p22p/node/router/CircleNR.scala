package org.fc.brewchain.p22p.node.router

import scala.collection.mutable.Set
import scala.collection.Map
import org.apache.commons.lang3.StringUtils
import onight.oapi.scala.traits.OLog
import scala.concurrent.blocking
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.p22p.node.PNode
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.NodeInstance

case class CMSetInfo(nodecount: Int, circleMap: Map[Int, Set[Int]])

object CircleNR extends MessageRouter with OLog {
  def getRand() = DHTConsRand.getRandFactor()

  private var cminfo: CMSetInfo = CMSetInfo(0, Map.empty);

  def cmInfo(): CMSetInfo = cminfo
  def resetMap(n: Int = 10) = {
    cminfo = CMSetInfo(n, CMSCalc.markCircleSets(n).toMap)
  }

  override def broadcastMessage(packet: FramePacket, from: PNode = NodeInstance.root)(implicit to: PNode = NodeInstance.root,
    nextHops: IntNode = FullNodeSet(),
    network: Network = Networks.instance): Unit = {
    //    log.debug("broadcastMessage:cur=@" + to.node_idx + ",from.idx=" + from.node_idx + ",netxt=" + nextHops)
    to.counter.recv.incrementAndGet();
    network.updateConnect(from.node_idx, to.node_idx)

    to.processMessage(packet, from)
    nextHops match {
      case f: FullNodeSet =>
        //from begin
        val (treere, result) = CMSCalc.calcRouteSets(to.node_idx)(cminfo.circleMap)
        //        log.debug("CMSCalc:" + treere)
        network.nodeByIdx(treere.fromIdx) match {
          case Some(n) =>
            treere.treeHops.nodes.map { nids =>
              routeMessage(packet)(n, nids, network)
            }
          case _ =>
            log.warn("not found id:" + (treere.fromIdx))
        }
      case none: EmptySet =>
        log.debug("Leaf Node");
      case ns: NodeSet =>
        ns.nodes.map { nids =>
          routeMessage(packet)(to, nids, network)
        }
      case subset: IntNode =>
        routeMessage(packet)(to, subset, network)
    }

  }
  override def routeMessage(packet: FramePacket)(implicit from: PNode, //
    nextHops: IntNode,
    network: Network) {
    //    log.debug("routeMessage:from=" + from.node_idx + ",next=" + nextHops)
    from.counter.send.incrementAndGet()
    nextHops match {
      case ts: DeepTreeSet =>
        //        log.debug(" route :DeepTreeSet:" + ts)
        network.nodeByIdx(ts.fromIdx) match {
          case Some(n) =>
            //            ts.treeHops.nodes.map { nid =>
            //            log.debug("castSet:" + ts.treeHops + ",from=" + n.node_idx)
            broadcastMessage(packet, from)(n, ts.treeHops, network)
          //            }
          case _ =>
            log.warn("not found id:" + (ts.fromIdx))
        }

      case _ =>
        log.warn("unknow Set," + nextHops)
    }

  }

}