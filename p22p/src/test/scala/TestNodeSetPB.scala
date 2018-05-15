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
import org.fc.brewchain.p22p.utils.NodeSetHelper
import org.fc.brewchain.p22p.node.router.CMSCalc
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBNode
import org.fc.brewchain.p22p.pbgens.P22P.PSRouteMessage.PBDeepTreeSet
import com.google.protobuf.Message
import java.util.concurrent.CountDownLatch

object TestNodeSetPB extends OLog with NodeSetHelper {

  def main(args: Array[String]): Unit = {
    val nodeCount = 10
    var bitenc = BigInt(0)
    for (i <- 0 to nodeCount - 1) {
      bitenc = bitenc.setBit(i * 2);
    }
    val circleNR = CircleNR(bitenc);
    val (treere, result) = CMSCalc.calcRouteSets(0)(circleNR.cminfo.circleMap)
    val pbo = scala2pb(treere);
    println(pbo.getData.size());
    val treess = pb2scala(pbo);
    println("tree=" + treess);
    val any = com.google.protobuf.Any.pack(pbo);
    println("any=" + any + ":" + any.getParserForType)
    val anypb = PBNode.newBuilder().mergeFrom(any.toByteString()).build();

    println("any.pb=" + anypb)
    val cdl=new CountDownLatch(6);

    val treessany = pb2scala(anypb);
    println("any.pb=" + treessany)

  }
}