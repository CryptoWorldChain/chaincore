package org.fc.brewchain.p22p.node.router

import scala.collection.mutable.Set
import scala.collection.Map
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.p22p.node.PNode
import org.fc.brewchain.p22p.node.NodeInstance
import scala.math.BigInt
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.Networks
import onight.oapi.scala.traits.OLog

object RandomNR extends MessageRouter with OLog {
  def getRand() = Math.random(); //DHTConsRand.getRandFactor()

  override def routeMessage(packet: FramePacket)(implicit from: PNode = NodeInstance.root, //
    nextHops: IntNode = FullNodeSet(),
    network: Network = Networks.instance) {
    //    log.debug("routeMessage:from=" + from.node_idx + ",next=" + nextHops)
    nextHops match {
      case fs: FlatSet =>
        val nextHopsCount = fs.nextHops.bitCount
        val (directCount, eachsetCount) = getDiv(nextHopsCount);
        //        var ran = ((getRand() * nextHopsCount) % directCount).asInstanceOf[Int];
        val mapSets = scala.collection.mutable.Map.empty[Int, BigInt]; //leader==>follow
        val startNodeSets = Set.empty[(Int, PNode)]; //leader==>follow
        val offset = (getRand() * nextHopsCount).asInstanceOf[Int];
        var i: Int = offset;
        //        log.debug("nextHopsCount==" + nextHopsCount + ",directCount=" + directCount + ",eachsetCount=" + eachsetCount)
        //ffmapSets.clear()
        network.directNodes.filter(node => fs.nextHops.testBit(node.node_idx) && from.node_idx != node.node_idx)
          .map({ node =>
            i = i + 1;
            var setid = ((i) % directCount);
//            if (fs.nextHops.bitCount == network.node_bits.bitCount)
//              println("node to broadcast==" + node + ",setid=" + setid)
            mapSets.get(setid) match {
              case Some(v: BigInt) =>
                if (!v.testBit(node.node_idx)) {
                  mapSets.put(setid, v.setBit(node.node_idx));
                  //            v.nextHops = v.nextHops.setBit(node.node_idx);
                }
              case _ =>
                mapSets.put(setid, BigInt(0));
                startNodeSets.add((setid, node))
            }
          })
        if (fs.nextHops.bitCount == network.node_bits.bitCount) {
//          log.debug("nextHopsCount==" + nextHopsCount + ",directCount=" + directCount + ",eachsetCount=" + eachsetCount + ",offset=" + offset)
//          println("startNodes==" + startNodeSets + ",mapSets=" + mapSets)
        }
        startNodeSets.map { sn =>
          val (setid, node) = sn
          broadcastMessage(packet, from)(node,
            FlatSet(node.node_idx, mapSets.getOrElse(setid, BigInt(0))), network)
        }
      //
      case n @ _ =>
        log.warn("cannot route not flat Set:" + n)
    }

  }

  def getDiv(n: Int): (Int, Int) = {
    val d: Int = Math.sqrt(n.asInstanceOf[Double]).asInstanceOf[Int];
    var i = d;
    for (i <- d until 1 by -1) {
      if (n % i == 0) {
        //        log.debug("d=" + d + "==>" + (i, n / i));
        return (i, n / i);
      }
    }
    //    log.debug("d.1=" + d + "==>" + (1, n));
    (1, n);
  }

}