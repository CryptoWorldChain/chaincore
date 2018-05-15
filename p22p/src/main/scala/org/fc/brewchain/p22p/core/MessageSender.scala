package org.fc.brewchain.p22p.core

import onight.tfw.otransio.api.PSender
import onight.tfw.otransio.api.IPacketSender
import scala.beans.BeanProperty
import onight.osgi.annotation.NActorProvider
import com.google.protobuf.Message
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.async.CallBack
import onight.tfw.ntrans.api.NActor
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.node.PNode
import onight.tfw.otransio.api.PackHeader
import org.fc.brewchain.bcapi.BCPacket
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.node.Networks
import com.google.protobuf.MessageOrBuilder

import org.brewchain.bcapi.utils.PacketIMHelper._
import scala.collection.TraversableLike
import onight.tfw.otransio.api.NonePackSender
import org.fc.brewchain.p22p.node.Network
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.node.Node
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.otransio.api.PSenderService
import onight.tfw.ntrans.api.ActorService

@NActorProvider
@Provides(specifications = Array(classOf[ActorService],classOf[PSenderService]))
class CMessageSender extends NActor {

  //http. socket . or.  mq  are ok
  @PSender
  var sockSender: IPacketSender = new NonePackSender();

  def setSockSender(send: IPacketSender): Unit = {
    sockSender = send;
    MessageSender.sockSender = sockSender;
  }
  def getSockSender(): IPacketSender = {
    sockSender
  }
}

object MessageSender extends  OLog{
  var sockSender: IPacketSender = new NonePackSender();
  def appendUid(pack: BCPacket, node: Node)(implicit network: Network): Unit = {
    if (network.isLocalNode(node)) {
      pack.getExtHead.remove(PackHeader.PACK_TO);
    } else {
      pack.putHeader(PackHeader.PACK_TO, node.bcuid);
      pack.putHeader(PackHeader.PACK_URI, node.uri);
    }
    pack.putHeader(PackHeader.PACK_FROM, network.root().bcuid);
  }

  def sendMessage(gcmd: String, body: Message, node: Node, cb: CallBack[FramePacket])(implicit network: Network) {
    val pack = BCPacket.buildSyncFrom(body, gcmd.substring(0, 3), gcmd.substring(3));
    appendUid(pack, node)
    log.trace("sendMessage:" + pack.getModuleAndCMD + ",F=" + pack.getFrom() + ",T=" + pack.getTo())
    sockSender.asyncSend(pack, cb)
  }

  def wallMessageToPending(gcmd: String, body: Message)(implicit network: Network) {
    val pack = BCPacket.buildAsyncFrom(body, gcmd.substring(0, 3), gcmd.substring(3));
    log.trace("wallMessage:" + pack.getModuleAndCMD + ",F=" + pack.getFrom() + ",T=" + pack.getTo())
    network.pendingNodes.map { node =>
      appendUid(pack, node)
      sockSender.post(pack)
    }
  }

  def wallMessageToPending(gcmd: String, body: ByteString)(implicit network: Network) {
    val pack = BCPacket.buildAsyncFrom(body.toByteArray(), gcmd.substring(0, 3), gcmd.substring(3));
    log.trace("wallMessage:" + pack.getModuleAndCMD + ",F=" + pack.getFrom() + ",T=" + pack.getTo())
    network.pendingNodes.map { node =>
      appendUid(pack, node)
      sockSender.post(pack)
    }
  }

  def postMessage(gcmd: String, body: Either[Message, ByteString], node: Node)(implicit network: Network): Unit = {
    //    if("TTTPZP".equals(gcmd)){
    //      return;
    //    }
    val pack = body match {
      case Left(m) => BCPacket.buildAsyncFrom(m, gcmd.substring(0, 3), gcmd.substring(3));
      case Right(b) => BCPacket.buildAsyncFrom(b.toByteArray(), gcmd.substring(0, 3), gcmd.substring(3));
    }
    appendUid(pack, node)
    //    log.trace("postMessage:" + pack)
    //    log.trace("postMessage:" + pack.getModuleAndCMD + ",F=" + pack.getFrom() + ",T=" + pack.getTo())
    sockSender.post(pack)
  }

  def replyPostMessage(gcmd: String, node: Node, body: Message)(implicit network: Network) {
    val pack = BCPacket.buildAsyncFrom(body, gcmd.substring(0, 3), gcmd.substring(3));
    appendUid(pack, node); //frompack.getExtStrProp(PackHeader.PACK_FROM));
    sockSender.post(pack)
  }

  def dropNode(node: Node) {
    sockSender.tryDropConnection(node.bcuid);
  }

  def changeNodeName(oldName: String, newName: String) {
    sockSender.changeNodeName(oldName, newName);
  }

  def setDestURI(bcuid: String, uri: String) {
    sockSender.setDestURI(bcuid, uri);
  }
}


