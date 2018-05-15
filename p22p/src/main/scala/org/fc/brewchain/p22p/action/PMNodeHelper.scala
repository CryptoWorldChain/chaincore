package org.fc.brewchain.p22p.action

import org.fc.brewchain.p22p.node.PNode
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfoOrBuilder
import org.fc.brewchain.p22p.stat.MessageCounter.CCSet
import onight.tfw.outils.serialize.ProtobufSerializer
import onight.tfw.outils.serialize.ISerializer
import onight.tfw.outils.serialize.SerializerFactory
import org.apache.commons.codec.binary.Base64
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.Node

trait PMNodeHelper {

  def networkByID(nid: String): Network = {
    Networks.networkByID(nid);
  }

  def toPMNode(n: Node): PMNodeInfo.Builder = {
    PMNodeInfo.newBuilder()
      .setUri(n.uri)
      .setNodeName(n.name)
      .setNodeIdx(n.node_idx)
      .setSign(n.sign)
      .setPubKey(n.pub_key).setStartupTime(n.startup_time).setTryNodeIdx(n.try_node_idx).setBcuid(n.bcuid)
      .setSendCc(n.counter.send.get).setRecvCc(n.counter.recv.get).setBlockCc(n.counter.blocks.get)
  }

  def toFullPMNode(n: Node): PMNodeInfo.Builder = {
    PMNodeInfo.newBuilder().setUri(n.uri).setNodeName(n.name)
      .setSign(n.sign)
      .setPubKey(n.pub_key).setStartupTime(n.startup_time).setTryNodeIdx(n.try_node_idx).setBcuid(n.bcuid)
      .setPriKey(n.pri_key).setNodeIdx(n.node_idx)
      .setSendCc(n.counter.send.get).setRecvCc(n.counter.recv.get).setBlockCc(n.counter.blocks.get)
  }
  val pser = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_PROTOBUF)

  def serialize(n: Node): String = {
    Base64.encodeBase64String(toBytes(toFullPMNode(n)))
  }

  def deserialize(str: String,_uri:String = null): PNode = {
    fromPMNode(pser.deserialize(Base64.decodeBase64(str), classOf[PMNodeInfo]),_uri)
  }

  def fromPMNode(pm: PMNodeInfoOrBuilder, _overrided_uri: String = null): PNode = {
    PNode(
      _name = pm.getNodeName, _node_idx = pm.getNodeIdx, //node info
      _sign = pm.getSign,
      _uri =
        if (_overrided_uri == null) pm.getUri else _overrided_uri, //
      _startup_time = pm.getStartupTime, //
      _pub_key = pm.getPubKey, //
      _counter = new CCSet(pm.getRecvCc, pm.getSendCc, pm.getBlockCc),
      _try_node_idx = pm.getTryNodeIdx,
      _bcuid = pm.getBcuid,
      _pri_key = pm.getPriKey)
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

