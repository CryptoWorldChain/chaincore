package org.fc.brewchain.p22p.node.router

import java.math.BigInteger
import scala.collection.immutable.Set
import com.google.protobuf.Message
import org.fc.brewchain.p22p.node.PNode
import onight.tfw.otransio.api.beans.FramePacket
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.Network
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.node.Node

trait MessageRouter extends OLog {

  def broadcastMessage(gcmd:String,body: Either[Message,ByteString], from: Node)(implicit to: Node,
    nextHops: IntNode = FullNodeSet(),
    network: Network,messageid:String): Unit = {
//        log.debug("broadcastMessage:cur=@" + to.node_idx + ",from.idx=" + from.node_idx + ",netxt=" + nextHops)
    to.counter.recv.incrementAndGet();
    //    from.counter.send.incrementAndGet();
//    network.updateConnect(from.node_idx,to.node_idx)
     
    to.processMessage(gcmd,body)
    nextHops match {
      case f: FullNodeSet =>
        from.counter.send.incrementAndGet();
        routeMessage(gcmd,body)(to, FlatSet(from.node_idx, network.node_bits), network,messageid)
      case none: EmptySet =>
        log.debug("Leaf Node");
      case subset: IntNode =>
        from.counter.send.incrementAndGet();
        routeMessage(gcmd,body)(to, subset, network,messageid)
    }

  }

  def routeMessage(gcmd:String,body: Either[Message,ByteString])(implicit from: Node,
    nextHops: IntNode = FullNodeSet(), 
    network: Network,messageid:String)
}




