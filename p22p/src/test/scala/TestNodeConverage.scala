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
import java.util.HashMap
import org.fc.brewchain.p22p.stat.MessageCounter
import org.fc.brewchain.p22p.node.router.DHTConsRand
import org.fc.brewchain.p22p.node.router.RandomNR
import onight.tfw.otransio.api.PacketHelper
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.router.CircleNR

object TestNodeConverge extends OLog {

  def main(args: Array[String]): Unit = {
    val nodeCount = 1000;
    val nodes = new ListBuffer[PNode]();
    val networks = new ListBuffer[Network]();
    val nodesMap = new HashMap[String, PNode]();
    for (i <- 0 to nodeCount - 1) {
      val node = new PNode(name = "a" + i, node_idx = i);
      nodes.+=(node);
      networks.append(new Network())
    }
    networks.map { net =>
      nodes.map { node =>
        net.addDNode(node);
      }
    }
    CircleNR.resetMap(nodeCount);

    // cut nodes
    Scheduler.scheduleWithFixedDelay(new Runnable {
      def run() = {
        //drop more than 10 connections
        val totalrecv = nodes.foldLeft(0L)((a, b) => a + b.counter.recv.get)
        val totalsend = nodes.foldLeft(0L)((a, b) => a + b.counter.send.get)

        println("totalSend:" + totalsend + ",totalrecv=" + totalrecv);
        var maxconn = 0
        networks.map { net =>
          if (net.connectedMap.size != nodes.size + 1) {
//            println("netmapsize. =:" + net.connectedMap)
          }
          net.connectedMap.map(conns =>
            if (maxconn < conns._2.size) {
              maxconn = conns._2.size
            })
        }

        networks.map { net =>
          net.connectedMap.map(conns =>
            if (maxconn == conns._2.size) {
//              println("maxMap. =:" + conns)
            })
        }

        println("max maxconn. =:" + maxconn)
        nodes.map { n =>
          val cc = new AtomicInteger(0);
          //          n.directNode.filter { f =>
          //            if (f._2.connected) {
          //              cc.incrementAndGet()
          //            }
          //            //            println("cc=" + cc.get)
          //            f._2.connected == true && cc.get > maxConnect
          //          }
          //            .map { f =>
          //              //drop one
          //              //              println("drop one:" + f._1 + ",from=" + n.name + ",cccount=" + cc.get)
          //              //  f._2.connected = false;
          //            }
          //          println(n.name + ":" + n.directNode.filter(_._2.connected).foldLeft("")((a, b) => a + "," + b._1));
        }
      }
    }, 5, 5, TimeUnit.SECONDS)
    val rootn = PNode("ROOT", -1);
    Scheduler.scheduleWithFixedDelay(new Runnable {
      def run() = {
        val net = networks((Math.random() * nodeCount % nodeCount).asInstanceOf[Int]);
        val n = net.nodeByIdx((Math.random() * nodeCount % nodeCount).asInstanceOf[Int]).get;

//      RandomNR.broadcastMessage(PacketHelper.genSyncPack("TEST", "ABC", "hello"),rootn)(n, network = net)
        CircleNR.broadcastMessage(PacketHelper.genSyncPack("TEST", "ABC", "hello"), rootn)(n, network = net)
        //        node.forwardMessage("aaa", msg, node.directNode.keys, node);
      }
    }, 1, 100, TimeUnit.MICROSECONDS)

    var start = System.currentTimeMillis();
    val sendcc = 10;
    val msg = PVBase.newBuilder().setMessageUid(UUIDGenerator.generate()).build()
    for (i <- 1 to sendcc) {
      val node = nodes(0); //(Math.random() * nodeCount % nodeCount).asInstanceOf[Int]);
      //      node.forwardMessage("aaa", msg, node.directNode.keys, node);
    }
    log.debug("cost=" + (System.currentTimeMillis() - start))

  }
}