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

class Network(_root: PNode = NodeInstance.root()) extends OLog //
{
  val try_node_idx: Int = NodeHelper.getCurrNodeIdx;
  private val directNodeByName: Map[String, PNode] = Map.empty[String, PNode];
  private val directNodeByIdx: Map[Int, PNode] = Map.empty[Int, PNode];
  private val pendingNodeByName: Map[String, PNode] = Map.empty[String, PNode];

  var node_bits = BigInt(0)
  def nodeByName(name: String): Node = directNodeByName.getOrElse(name, NoneNode());
  def nodeByIdx(idx: Int) = directNodeByIdx.get(idx);

  def directNodes: Iterable[PNode] = directNodeByName.values

  def root: PNode = _root
  def pendingNodes: Iterable[PNode] = directNodeByName.values

  def addDNode(node: PNode): Option[PNode] = {

    if (!directNodeByName.contains(node.name) && node.node_idx >= 0 && !node_bits.testBit(node.node_idx)) {
      directNodeByName.put(node.name, node)
      node_bits = node_bits.setBit(node.node_idx);
      directNodeByIdx.put(node.node_idx, node);
    } else {
      None
    }
  }
  //  var node_idx = _node_idx; //全网确定之后的节点id

  def addPendingNode(node: PNode) {
    this.synchronized {
      if (node.name == root.name) {
        throw new NodeInfoDuplicated("same node with currnt node" + node.name + "@" + root.name);
      }
      if (directNodeByName.contains(node.name)) {
        throw new NodeInfoDuplicated("directNode exists Pending name=" + node.name + "@" + root.name);
      }
      pendingNodeByName.put(node.name, node);
      log.debug("addpending:" + pendingNodeByName.size + ",p=" + pendingNodeByName)
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
}



