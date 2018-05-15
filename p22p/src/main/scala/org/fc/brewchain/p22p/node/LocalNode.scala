package org.fc.brewchain.p22p.node

import onight.tfw.mservice.NodeHelper
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
import com.google.protobuf.StringValue
import com.google.protobuf.ByteString
import org.brewchain.bcapi.gens.Oentity.OKey
import org.slf4j.MDC
import org.fc.brewchain.p22p.utils.LogHelper

trait LocalNode extends OLog with PMNodeHelper with LogHelper {
  //  val node_name = NodeHelper.getCurrNodeName
  def netid(): String

  def NODE_ID_PROP = "org.bc.pzp." + netid() + ".node.id"
  def PROP_NODE_INFO = "org.bc.pzp." + netid() + ".node.info";

  private var rootnode: Node = PNode.NoneNode;

  def root(): Node = rootnode;

  def isLocalNode(node: Node): Boolean = {
    node == root || root.bcuid.equals(node.bcuid)
  }

  def isLocalNode(bcuid: String): Boolean = {
    root.bcuid.equals(bcuid)
  }

  def getFromDB(key: String, defaultv: String): String = {
    val v = Daos.odb.get(OKey.newBuilder().setData(ByteString.copyFrom(key.getBytes)).build())
    if (v == null || v.get() == null) {
      val prop = NodeHelper.getPropInstance.get(key, defaultv);
      NodeHelper.envInEnv(prop)
    } else {
      v.get.getInfo
    }
  }

  def syncInfo(node: Node): Boolean = {
    if (Daos.odb == null) return false;
    Daos.odb.putInfo(PROP_NODE_INFO, serialize(node));
    true
  }
  def newNode(nodeidx: Int = -1): PNode = {
    val kp = Daos.enc.genKeys()
    val newroot = PNode.signNode(NodeHelper.getCurrNodeName, node_idx = -1,
      uri = "tcp://" + NodeHelper.getCurrNodeListenOutAddr + ":" + NodeHelper.getCurrNodeListenOutPort,
      System.currentTimeMillis(), pub_key = kp.getPubkey,
      try_node_idx = nodeidx,
      bcuid = netid().head.toUpper+kp.getBcuid,
      pri_key = kp.getPrikey,
      v_address = kp.getAddress  
    )
    syncInfo(newroot)
    newroot;
  }
  def initNode() = {
    this.synchronized {
      if (rootnode == PNode.NoneNode) //second entry
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
                deserialize(node_info,"tcp://" + NodeHelper.getCurrNodeListenOutAddr + ":" + NodeHelper.getCurrNodeListenOutPort)
              }
              log.info("load node from db:" + r.bcuid + ",idx=" + r.node_idx)
              r
            } catch {
              case e: Throwable =>
                val r = newNode(PNode.genIdx());
                log.debug("new node info:" + r.bcuid + ",idx=" + r.node_idx)
                r
            }

          if (MessageSender.sockSender != null && rootnode != null && rootnode.bcuid != null) {
            MessageSender.sockSender.setCurrentNodeName(rootnode.bcuid)
          }

        } catch {
          case e: Throwable =>
            log.warn("unknow Error.", e)
        } finally {
          if (root() != null) {
            MDCSetBCUID(root().bcuid)
          }
        }
      }
    }
  }
  def initClusterNode(subnetRoot:Node) = {
    this.synchronized {
      initNode();
      rootnode = ClusterNode(net_id = netid(),
        cnode_idx = -1, _sign = "", 
        pnodes = Array(subnetRoot),
        _net_bcuid = rootnode.bcuid,
        _try_cnode_idx = rootnode.try_node_idx);
      if (rootnode == PNode.NoneNode) //second entry
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

          if (MessageSender.sockSender != null && rootnode != null && rootnode.bcuid != null) {
            MessageSender.sockSender.setCurrentNodeName(rootnode.bcuid)
          }

        } catch {
          case e: Throwable =>
            log.warn("unknow Error.", e)
        } finally {
          if (root() != null) {
            MDCSetBCUID(root().bcuid)
          }
        }
      }
    }
  }
  def resetRoot(node: Node): Unit = {
    this.rootnode = node;
  }
  def changeNodeIdx(test_bits: BigInt = BigInt("0")): Int = {
    this.synchronized {
      var v = 0;
      do {
        v = PNode.genIdx()
      } while (rootnode.node_idx == v || test_bits.testBit(v))

      Daos.odb.putInfo(NODE_ID_PROP, String.valueOf(v))
      rootnode = rootnode.changeIdx(v)
      syncInfo(rootnode)
      MDCSetBCUID(root().bcuid)
      log.debug("changeNode Index=" + v)
      v
    }
  }

}
