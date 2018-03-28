package org.fc.brewchain.p22p.action

import org.fc.brewchain.p22p.node.PNode
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfoOrBuilder
import org.fc.brewchain.p22p.node.NodeInstance
import org.fc.brewchain.p22p.stat.MessageCounter.CCSet
import onight.tfw.outils.serialize.ProtobufSerializer
import onight.tfw.outils.serialize.ISerializer
import onight.tfw.outils.serialize.SerializerFactory
import org.apache.commons.codec.binary.Base64
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.ByteString

trait PMNodeHelper {

  def toPMNode(n: PNode = NodeInstance.root()): PMNodeInfo.Builder = {
    PMNodeInfo.newBuilder().setAddress(n.address).setNodeName(n.name).setPort(n.port)
      .setProtocol(n.protocol)
      .setPubKey(n.pub_key).setStartupTime(n.startup_time).setTryNodeIdx(n.try_node_idx).setBcuid(n.bcuid)
  }

  def toFullPMNode(n: PNode = NodeInstance.root()): PMNodeInfo.Builder = {
    PMNodeInfo.newBuilder().setAddress(n.address).setNodeName(n.name).setPort(n.port)
      .setProtocol(n.protocol)
      .setPubKey(n.pub_key).setStartupTime(n.startup_time).setTryNodeIdx(n.try_node_idx).setBcuid(n.bcuid)
      .setPriKey(n.pri_key)
      .setSendCc(n.counter.send.get).setRecvCc(n.counter.recv.get).setBlockCc(n.counter.blocks.get)
  }
  val pser = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_PROTOBUF)

  def serialize(n: PNode = NodeInstance.root()): String = {
    Base64.encodeBase64String(toBytes(toFullPMNode(n)))
  }

  def deserialize(str: String): PNode = {
    fromPMNode(pser.deserialize(Base64.decodeBase64(str), classOf[PMNodeInfo]))
  }

  def fromPMNode(pm: PMNodeInfoOrBuilder): PNode = {
    PNode(
      name = pm.getNodeName, node_idx = pm.getNodeIdx, //node info
      protocol = pm.getProtocol, address = pm.getAddress, port = pm.getPort, //
      startup_time = pm.getStartupTime, //
      pub_key = pm.getPubKey, //
      counter = new CCSet(pm.getRecvCc, pm.getSendCc, pm.getBlockCc),
      try_node_idx = pm.getTryNodeIdx,
      bcuid = pm.getBcuid,
      pri_key = pm.getPriKey)
  }

  def toBytes(body: MessageOrBuilder): Array[Byte] = {
    pser.serialize(body).asInstanceOf[Array[Byte]]
  }
  def toByteSting(body: MessageOrBuilder): ByteString = {
    ByteString.copyFrom(toBytes(body))
  }

  def fromByteSting[T](str: ByteString, clazz: Class[T]): T = {
    pser.deserialize(str.toByteArray(), clazz)
  }

}