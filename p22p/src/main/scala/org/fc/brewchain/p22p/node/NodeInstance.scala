package org.fc.brewchain.p22p.node

import onight.tfw.mservice.NodeHelper
import org.fc.brewchain.bcapi.crypto.KeyPair
import org.fc.brewchain.bcapi.crypto.EncHelper
import org.apache.commons.lang3.StringUtils
//import org.spongycastle.util.encoders.Hex
//import org.ethereum.crypto.HashUtil
import org.fc.brewchain.p22p.Daos
import java.math.BigInteger
import onight.oapi.scala.traits.OLog
import com.google.protobuf.Message
import org.fc.brewchain.p22p.core.MessageSender
import org.fc.brewchain.bcapi.crypto.BCNodeHelper
import org.fc.brewchain.p22p.action.PMNodeHelper

object NodeInstance extends OLog with PMNodeHelper {
  //  val node_name = NodeHelper.getCurrNodeName
  val NODE_ID_PROP = "org.bc.node.id"
  val PROP_NODE_INFO = "zp.bc.node.info";

  private var rootnode: PNode = null;

  def root(): PNode = rootnode;

  def getNodeIdx: Int = rootnode.node_idx

  def isLocalNode(node: PNode): Boolean = {
    node == root || root.bcuid.equals(node.bcuid)
  }
  def getFromDB(key: String, defaultv: String): String = {
    val v = Daos.odb.get(key)
    if (v == null || v.get() == null || v.get.getValue == null) {
      val prop = NodeHelper.getPropInstance.get(key, defaultv);
      NodeHelper.envInEnv(prop)
    } else {
      v.get.getValue.trim()
    }

  }

  def syncInfo(node: PNode): Boolean = {
    if (Daos.odb == null) return false;
    Daos.odb.put(PROP_NODE_INFO, serialize(node));
    true
  }
  def isReady(): Boolean = {
    log.debug("check Node Instace:Daos.odb=" + Daos.odb)
    if (Daos.odb == null) return false;
    if (rootnode == null)
      initNode()
    if (MessageSender.sockSender != null && rootnode != null && rootnode.bcuid != null) {
      MessageSender.sockSender.setCurrentNodeName(rootnode.bcuid)
    }
    rootnode != null;
  }
  def newNode(nodeidx: Int = -1): PNode = {
    val kp = EncHelper.newKeyPair()
    val newroot = PNode(NodeHelper.getCurrNodeName, node_idx = nodeidx, protocol = "tcp",
      address = NodeHelper.getCurrNodeListenOutAddr,
      NodeHelper.getCurrNodeListenOutPort,
      System.currentTimeMillis(), kp.pubkey,
      try_node_idx = nodeidx,
      bcuid = kp.bcuid,
      pri_key = kp.prikey)
    syncInfo(newroot)
    newroot;
  }
  def initNode() = {
    this.synchronized {
      if (rootnode == null) //second entry
      {
        try {
          val nodeidx = PNode.genIdx()
          val node_info = getFromDB(PROP_NODE_INFO, "");
          rootnode =
            try {
              log.info("load node from db info=:" + node_info)
              val r = if (StringUtils.isBlank(node_info)) {
                newNode(PNode.genIdx());
              } else {
                deserialize(node_info)
              }
              log.info("load node from db:" + r.bcuid + ",idx=" + r.node_idx)
              r
            } catch {
              case e: Throwable =>
                val r = newNode(PNode.genIdx());
                log.debug("new node info:" + r.bcuid + ",idx=" + r.node_idx)
                r
            }
        } catch {
          case e: Throwable =>
            log.warn("unknow Error.", e)
        }
      }
    }
  }
  def changeNodeIdx(test_bits: BigInteger = new BigInteger("0")): Int = {
    this.synchronized {
      var v = 0;
      do {
        v = PNode.genIdx()
      } while (getNodeIdx == v || test_bits.testBit(v))

      Daos.odb.put(NODE_ID_PROP, String.valueOf(v))
      rootnode = rootnode.changeIdx(v)
      syncInfo(rootnode)
      log.debug("changeNode Index=" + v)
      v
    }
  }

  def inNetwork(): Boolean = {
    getNodeIdx > 0 && Networks.instance.node_bits.bitCount >= 2;
  }

}
