import scala.collection.mutable.Map
import java.util.ArrayList
import org.fc.brewchain.p22p.node.PNode
import onight.oapi.scala.traits.OLog
import scala.collection.mutable.ListBuffer
import java.math.BigInteger
import org.fc.brewchain.bcapi.crypto.BitMap
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import onight.tfw.outils.serialize.UUIDGenerator
import org.fc.brewchain.p22p.tasks.Scheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.fc.brewchain.p22p.stat.MessageCounter
import java.util.HashMap
import org.fc.brewchain.p22p.node.router.RandomNR
import onight.tfw.otransio.api.PacketHelper
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.router.CircleNR
import com.google.protobuf.StringValue

object TestMemGraphy extends OLog  with BitMap{
  def main(args: Array[String]): Unit = {
    val nodeCount = 10;
    val nodes = new ListBuffer[PNode]();
    val networks = new ListBuffer[Network]();
    val nodesMap = new HashMap[String, PNode]();
    var bitenc = BigInt(0)
    for (i <- 0 to nodeCount - 1) {

      val node = new PNode(_name = "a" + i, _node_idx = i*2, "",
        _try_node_idx = i*2);
      //      println("idx=" + node.node_idx);
      nodes.+=(node);
      bitenc = bitenc.setBit(i*2);
      networks.append(new Network("test:"+i,""))
    }
    println("bitenc=" + bitenc.toString(2) + "==>" + hexToMapping(bitenc)
        +",size=="+nodes.size)
    
    networks.map { net =>
      net.resetRoot(nodes(0))
      nodes.map { node =>
        if(net.addDNode(node)==None){
          println("append error:"+node)
        }
      }
    }
    networks.map { net =>
      println("net.bitenc=" + net.node_bits.toString(2)+".len="+net.node_bits.bitLength
         +".cc="+net.node_bits.bitCount+".nodescount="+net.directNodes.size);

    }
//    networks(0).directNodes.map { node =>
//      println("n==" + node.name+",idx="+node.node_idx);
//    }
    //    nodes.map { node => println(node.directNode.size) }
    var start = System.currentTimeMillis();
    val sendcc = 1;
    val msg = PVBase.newBuilder().setMessageUid(UUIDGenerator.generate()).build()
    //    CircleNR.resetMap(nodeCount);
    val circleNR = CircleNR(bitenc)
    println("circleNR="+circleNR.ncount+","+circleNR.encbits.toString(2)+","+circleNR.idxMap)
    val rootn = PNode("ROOT", -1, "");
    for (i <- 1 to sendcc) {
      val net = networks(0); //(Math.random() * nodeCount % nodeCount).asInstanceOf[Int]);
      //      node.forwardMessage("aaa", msg, node.directNode.keys, node);
      val n = net.nodeByIdx(0).get;
      //CircleNR.resetMap(net.root.node_idx, nodeCount);
      //                  RandomNR.broadcastMessage(PacketHelper.genSyncPack("TEST", "ABC", "hello"))(net.nodeByIdx((Math.random() * nodeCount % nodeCount).asInstanceOf[Int]).get, network = net)
      circleNR.broadcastMessage("TTTPZP",Left(StringValue.newBuilder().setValue("abc").build()),net.root())( toN=net.root(),network = net,messageid="abc")
      //       if (net.connectedMap.size != nodes.size + 1) {
//      println("netmapsize. =:" + net.connectedMap)
      //          }
    }
    var maxconn = 0

    networks(0).connectedMap.map(conns =>
      if (maxconn < conns._2.size) {
        maxconn = conns._2.size
      })

    networks(0).connectedMap.map(conns =>
      if (maxconn == conns._2.size) {
        println("maxconn==" + conns)
      })

    log.debug("cost=" + (System.currentTimeMillis() - start) + ",maxconn=" + maxconn)
    //    log.debug("connmap=" + CircleNR.cmInfo())
    nodes.map { f1 =>
      if (f1.counter.recv.get != sendcc) {
        println("cannot get message:" + f1.name + "," + f1.counter.recv.get)
      }
    }
    val totalrecv = nodes.foldLeft(0L)((a, b) => a + b.counter.recv.get)
    val totalsend = nodes.foldLeft(0L)((a, b) => a + b.counter.send.get)
    println("totalSend:" + totalsend + ",totalrecv=" + totalrecv);

    var node_bits = new BigInteger("0")
    println(node_bits.toString() + "==>" + node_bits.testBit(2));

    val node_bits1 = node_bits.setBit(100).setBit(1).setBit(12).setBit(0);

    println(node_bits1.toString(16) + "==>" + node_bits1.testBit(100) + "==>" + hexToMapping(node_bits1));
    println(node_bits1.bitCount() + "," + node_bits1.bitLength());

  }
}