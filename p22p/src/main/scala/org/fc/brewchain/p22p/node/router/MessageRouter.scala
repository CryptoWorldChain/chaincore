package org.fc.brewchain.p22p.node.router

import java.math.BigInteger
import scala.collection.immutable.Set
import com.google.protobuf.Message
import org.fc.brewchain.p22p.node.PNode
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.p22p.node.NodeInstance
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.Network

trait MessageRouter extends OLog {

  def broadcastMessage(packet: FramePacket, from: PNode = NodeInstance.root)(implicit to: PNode = NodeInstance.root,
    nextHops: IntNode = FullNodeSet(),
    network: Network = Networks.instance): Unit = {
//        log.debug("broadcastMessage:cur=@" + to.node_idx + ",from.idx=" + from.node_idx + ",netxt=" + nextHops)
    to.counter.recv.incrementAndGet();
    //    from.counter.send.incrementAndGet();
    network.updateConnect(from.node_idx,to.node_idx)
      
    to.processMessage(packet, from)
    nextHops match {
      case f: FullNodeSet =>
        from.counter.send.incrementAndGet();
        routeMessage(packet)(to, FlatSet(from.node_idx, network.node_bits), network)
      case none: EmptySet =>
        log.debug("Leaf Node");
      case subset: IntNode =>
        from.counter.send.incrementAndGet();
        routeMessage(packet)(to, subset, network)
    }

  }

  def routeMessage(packet: FramePacket)(implicit from: PNode = NodeInstance.root,
    nextHops: IntNode = FullNodeSet(),
    network: Network = Networks.instance)
}




