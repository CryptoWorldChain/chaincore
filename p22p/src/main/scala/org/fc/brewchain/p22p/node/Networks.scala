package org.fc.brewchain.p22p.node

import org.fc.brewchain.p22p.exception.NodeInfoDuplicated
import java.net.URL
import org.apache.commons.lang3.StringUtils
import onight.tfw.mservice.NodeHelper
import com.google.protobuf.MessageOrBuilder
import org.fc.brewchain.p22p.core.MessageSender
import com.google.protobuf.Message
import scala.collection.mutable.Map
import onight.oapi.scala.traits.OLog
import scala.collection.Iterable
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.p22p.node.router.CircleNR

class Network() extends OLog //
{
  private val directNodeByName: Map[String, PNode] = Map.empty[String, PNode];
  private val directNodeByIdx: Map[Int, PNode] = Map.empty[Int, PNode];
  private val pendingNodeByName: Map[String, PNode] = Map.empty[String, PNode];

  val connectedMap: Map[Int, Map[Int, Int]] = Map.empty[Int, Map[Int, Int]];

  var node_bits = BigInt(0)
  def nodeByName(name: String): Node = directNodeByName.getOrElse(name, NoneNode());
  def nodeByIdx(idx: Int) = directNodeByIdx.get(idx);

  def directNodes: Iterable[PNode] = directNodeByName.values

  def pendingNodes: Iterable[PNode] = pendingNodeByName.values

  def addDNode(node: PNode): Option[PNode] = {

    if (!directNodeByName.contains(node.name) && node.node_idx >= 0 && !node_bits.testBit(node.node_idx)) {
      directNodeByName.put(node.name, node)
      node_bits = node_bits.setBit(node.node_idx);
      directNodeByIdx.put(node.node_idx, node);
    } else {
      None
    }
  }
  
   def removeDNode(node: PNode): Option[PNode] = {
    if (directNodeByName.contains(node.name) ) {
      node_bits = node_bits.clearBit(node.node_idx);
      directNodeByName.remove(node.name)
    } else {
      None
    }
  }
  //  var node_idx = _node_idx; //全网确定之后的节点id

  def addPendingNode(node: PNode): Boolean = {
    this.synchronized {
      //      if (node.name == root.name) {
      //        throw new NodeInfoDuplicated("same node with currnt node" + node.name + "@" + root.name);
      //      }
      if (directNodeByName.contains(node.name)) {
        log.debug("directNode exists in DirectNode name=" + node.name);
        false
      } else if (pendingNodeByName.contains(node.name)) {
        log.debug("pendingNode exists PendingNodes name=" + node.name);
        false
      } else {
        pendingNodeByName.put(node.name, node);
        log.debug("addpending:" + pendingNodeByName.size + ",p=" + node.name)
        true
      }
    }
  }
  
    def removePendingNode(node: PNode): Boolean = {
    this.synchronized {
      //      }
      if (!pendingNodeByName.contains(node.name)) {
        false
      } else {
        pendingNodeByName.remove(node.name);
        log.debug("remove:" + pendingNodeByName.size + ",p=" + node.name)
        true
      }
    }
  }

  def updateConnect(fromIdx: Int, toIdx: Int) = {
    if (fromIdx != -1 && toIdx != -1)
      connectedMap.synchronized {
        connectedMap.get(fromIdx) match {
          case Some(m) =>
            m.put(toIdx, fromIdx)
          case None =>
            connectedMap.put(fromIdx, scala.collection.mutable.Map[Int, Int](toIdx -> fromIdx))
        }
        connectedMap.get(toIdx) match {
          case Some(m) =>
            m.put(fromIdx, toIdx)
          case None =>
            connectedMap.put(toIdx, scala.collection.mutable.Map[Int, Int](fromIdx -> toIdx))
        }
        //      log.debug("map="+connectedMap)
      }
  }
  //  def isLocal() = (NodeInstance.curnode == this)
  //  override def processMessage(gcmd: String, msg: Message, from: PNode): Unit = {
  //    if (isLocal()) {
  //      log.debug("proc Local message");
  //    } else {
  //      log.debug("need to Send Message");
  //      MessageSender.postMessage(gcmd, msg, this); //(gcmd, body, node, cb)
  //    }
  //  }
}
object Networks {
  val instance: Network = new Network();
  def forwardMessage(fp: FramePacket) {
    CircleNR.broadcastMessage(fp)(NodeInstance.root(), network = instance)
  }
}



