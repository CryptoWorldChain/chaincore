package org.fc.brewchain.p22p.tasks

import java.util.concurrent.TimeUnit
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PBVoteNodeIdx
import java.math.BigInteger
import org.fc.brewchain.p22p.node.PNode
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.core.MessageSender
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CallBack
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import java.net.URL
import org.apache.felix.framework.URLHandlers
import org.fc.brewchain.bcapi.URLHelper
import org.fc.brewchain.p22p.action.PMNodeHelper
import org.fc.brewchain.bcapi.crypto.BitMap
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import org.fc.brewchain.p22p.pbgens.P22P.PBFTStage
import org.fc.brewchain.p22p.node.Networks
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.pbft.StateStorage
import org.fc.brewchain.p22p.pbgens.P22P.PVType
import onight.tfw.outils.serialize.UUIDGenerator
import org.fc.brewchain.p22p.utils.Config
import org.fc.brewchain.p22p.pbft.VoteQueue
import org.fc.brewchain.p22p.node.Network
import java.util.concurrent.atomic.AtomicBoolean

//投票决定当前的节点
case class VoteNodeMap(network: Network, voteQueue: VoteQueue) extends SRunner with BitMap {
  def getName() = "VoteNodeMap"
  val checking = new AtomicBoolean(false)

  def runOnce() = {
    log.debug("VoteNodeMap :Run----Try to Vote Node Maps");
    val oldThreadName = Thread.currentThread().getName + ""
    if (checking.compareAndSet(false, true)) {
      try {

        Thread.currentThread().setName("VoteNodeMap");
        log.info("CurrentPNodes:PendingSize=" + network.pendingNodes.size + ",DirectNodeSize=" + network.directNodes.size);
        val vbase = PVBase.newBuilder();

        vbase.setState(PBFTStage.PENDING_SEND)
        vbase.setMType(PVType.NETWORK_IDX)
        var pendingbits = BigInt(1)
        //init. start to vote.
        if (network.joinNetwork.pendingJoinNodes.size() / 2 > network.onlineMap.size
          || network.onlineMap.size <= 0) {
          log.info("cannot vote for pendingJoinNodes Size bigger than online half:PendJoin=" +
            network.joinNetwork.pendingJoinNodes.size() + ": Online=" + network.onlineMap.size)
          //for fast load
        } else if (network.stateStorage.nextV(vbase) > 0) {
          vbase.setMessageUid(UUIDGenerator.generate())
          vbase.setOriginBcuid(network.root().bcuid)
          vbase.setFromBcuid(network.root().bcuid);
          vbase.setLastUpdateTime(System.currentTimeMillis())
          vbase.setNid(network.netid);
          val vbody = PBVoteNodeIdx.newBuilder();
          var bits = network.node_bits;
          pendingbits = BigInt(0)

          network.pendingNodes.map(n =>
            //          if (network.onlineMap.contains(n.bcuid)) {
            if (bits.testBit(n.try_node_idx)) {
              log.debug("error in try_node_idx @n=" + n.name + ",try=" + n.try_node_idx + ",bits=" + bits);
            } else { //no pub keys
              pendingbits = pendingbits.setBit(n.try_node_idx);
              vbody.addPendingNodes(toPMNode(n));
            } //          }
            )

          vbody.setPendingBitsEnc(hexToMapping(pendingbits))
          vbody.setNodeBitsEnc(network.node_strBits)
          vbase.setContents(toByteSting(vbody))
          //      vbase.addVoteContents(Any.pack(vbody.build()))
          //      if (network.node_bits.bitCount <= 0) {
          //        log.debug("networks has not directnode!")
          log.info("vote -- Nodes:" + vbody.getNodeBitsEnc + ",pendings=" + vbody.getPendingBitsEnc);
          vbase.setV(vbase.getV);
          vbase.setN(network.pendingNodes.size + network.directNodes.size);
          if (vbase.getN > 0) {
            log.info("broadcast Vote Message:V=" + vbase.getV + ",N=" + vbase.getN + ",from=" + vbase.getFromBcuid
              + ",SN=" + vbase.getStoreNum + ",VC=" + vbase.getViewCounter + ",messageid=" + vbase.getMessageUid)
            val vbuild = vbase.build();
            //        Networks.wallMessage("VOTPZP", vbuild);
            voteQueue.appendInQ(vbase.setState(PBFTStage.PENDING_SEND).build())
          } else {
            log.debug("cannot start Vote N=0:")
          }
        }
        //      }
        //    NodeInstance.forwardMessage("VOTPZP", vbody.build());
        //vbody.setNodeBitsEnc(bits.toString(16));
        log.debug("Run-----[Sleep]"); //
        val sleepTime = if (pendingbits.bitCount > 0) {
          (Config.MAX_VOTE_SLEEP_MS - Config.MIN_VOTE_SLEEP_MS) + Config.MIN_VOTE_SLEEP_MS
        } else {
          (Config.MAX_VOTE_SLEEP_MS - Config.MIN_VOTE_SLEEP_MS) + Config.MIN_VOTE_WITH_NOCHANGE_SLEEP_MS
        }
        this.synchronized {
          this.wait((Math.random() * sleepTime + Config.MIN_VOTE_SLEEP_MS).asInstanceOf[Int]);
        }
      } catch {
        case e: Throwable =>
          log.warn("unknow Error:" + e.getMessage, e)
      } finally {
        checking.compareAndSet(true, false)
        Thread.currentThread().setName(oldThreadName);
      }
    }
  }
  //Scheduler.scheduleWithFixedDelay(new Runnable, initialDelay, delay, unit)
  //  def main(args: Array[String]): Unit = {
  //    URLHelper.init()
  //    //System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.bcapi.url");
  //    println(new URL("tcp://127.0.0.1:5100").getHost);
  //  }
}