package org.fc.brewchain.p22p.node

import onight.tfw.mservice.NodeHelper
import org.fc.brewchain.bcapi.crypto.KeyPair
import org.fc.brewchain.bcapi.crypto.EncHelper
import org.apache.commons.lang3.StringUtils
import org.spongycastle.util.encoders.Hex
import org.ethereum.crypto.HashUtil
import org.fc.brewchain.p22p.Daos
import java.math.BigInteger
import onight.oapi.scala.traits.OLog
import com.google.protobuf.Message
import org.fc.brewchain.p22p.core.MessageSender

object NodeInstance extends OLog {
  val node_name = NodeHelper.getCurrNodeName
  val NODE_ID_PROP = "org.bc.node.id"
  val kp = {
    val pubkey = NodeHelper.getPropInstance.get("zp.bc.node.pub", "");
    val prikey = NodeHelper.getPropInstance.get("zp.bc.node.pri", "");
    val address = NodeHelper.getPropInstance.get("zp.bc.node.addr", "");
    if (StringUtils.isBlank(pubkey)) {
      EncHelper.newKeyPair()
    } else {
      new KeyPair(
        pubkey,
        prikey,
        address,
        Hex.toHexString(HashUtil.ripemd160(Hex.decode(pubkey))));
    }

  }

  private var rootnode: PNode =
    PNode(name = node_name, node_idx = NodeHelper.getCurrNodeIdx, protocol = "tcp", address = NodeHelper.getCurrNodeID,
      NodeHelper.getCurrNodeListenOutPort,
      System.currentTimeMillis(), kp.pubkey);

  def root(): PNode = rootnode;
  
  def getNodeIdx: Int = rootnode.node_idx

  def isReady(): Boolean = {
    log.debug("check Node Instace:Daos.odb=" + Daos.odb)
    if (Daos.odb == null) return false;
    this.synchronized {
      if (NodeHelper.getCurrNodeIdx <= 0) {
        var v = Daos.odb.get(NODE_ID_PROP);
        val nodeidx = if (v == null || v.get() == null || !StringUtils.isNumeric(v.get.getValue)) {
          NodeHelper.getCurrNodeIdx();
        } else {
          log.debug("Load Node Instace Index from DB=" + v.get.getValue)
          Integer.parseInt(v.get.getValue);
        }
        Daos.odb.put(NODE_ID_PROP, String.valueOf(nodeidx))
        rootnode = rootnode.changeIdx(nodeidx)
        log.debug("Init Node Instace  Index=" + nodeidx)
      }
      return true;
    }
  }
  def changeNodeIdx(test_bits: BigInteger = new BigInteger("0")): Int = {
    this.synchronized {
      var v = 0;
      do {
        v = NodeHelper.resetNodeIdx();
      } while (getNodeIdx == v || test_bits.testBit(v))

      Daos.odb.put(NODE_ID_PROP, String.valueOf(v))
      rootnode = rootnode.changeIdx(v)
      log.debug("changeNode Index=" + v)
      v
    }
  }

  def forwardMessage(gcmd: String, msg: Message) {
    //    curnode.forwardMessage(gcmd, msg, Set.empty[String]);
    //    MessageSender.postMessage(gcmd, msg, node)
  }
  def inNetwork(): Boolean = {
    getNodeIdx > 0; //&& curnode.directNode.size == curnode.node_bits.bitCount();
  }

}
