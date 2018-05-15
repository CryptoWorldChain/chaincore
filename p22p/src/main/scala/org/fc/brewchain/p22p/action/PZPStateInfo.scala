package org.fc.brewchain.p22p.action

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.LService
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import org.fc.brewchain.bcapi.exception.FBSException
import org.apache.commons.lang3.StringUtils
import java.util.HashSet
import onight.tfw.outils.serialize.UUIDGenerator
import scala.collection.JavaConversions._
import org.apache.commons.codec.binary.Base64
import org.fc.brewchain.p22p.pbgens.P22P.PSJoin
import org.fc.brewchain.p22p.pbgens.P22P.PRetJoin
import org.fc.brewchain.p22p.PSMPZP
import org.fc.brewchain.p22p.pbgens.P22P.PCommand
import java.net.URL
import org.fc.brewchain.p22p.pbgens.P22P.PMNodeInfo
import org.fc.brewchain.p22p.exception.NodeInfoDuplicated
import org.fc.brewchain.p22p.pbgens.P22P.PSNodeInfo
import org.fc.brewchain.p22p.pbgens.P22P.PRetNodeInfo
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.pbgens.P22P.PSVoteState
import org.fc.brewchain.p22p.Daos
import org.fc.brewchain.p22p.pbft.StateStorage
import org.brewchain.bcapi.gens.Oentity.OValue
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import com.google.protobuf.ByteString
import org.fc.brewchain.p22p.pbgens.P22P.PRetVoteState
import org.fc.brewchain.p22p.pbgens.P22P.NodeStateInfo
import org.fc.brewchain.p22p.utils.LogHelper

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor],classOf[CMDService]
) )
class PZPStateInfo extends PSMPZP[PSVoteState] {
  override def service = PZPStateInfoService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PZPStateInfoService extends LogHelper with PBUtils with LService[PSVoteState] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSVoteState, handler: CompleteHandler) = {
    //    log.debug("onPBPacket::" + pbo)
    var ret = PRetVoteState.newBuilder();
    val network = networkByID(pbo.getNid)
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:" + pbo.getNid)
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      MDCSetBCUID(network)

      val ss = network.stateStorage;
      try {
        //       pbo.getMyInfo.getNodeName
        val strkey = pbo.getV match {
          case v if v > 0 => ss.STR_seq(pbo.getTValue) + ".F." + v
          case _ => ss.STR_seq(pbo.getTValue)
        }

        Daos.viewstateDB.get(strkey).get match {
          case ov if ov != null =>
            val pb = PVBase.newBuilder().mergeFrom(ov.getExtdata);
            pb.setContents(ByteString.copyFrom(Base64.encodeBase64(pb.getContents.toByteArray())))
            ret.setCur(NodeStateInfo.newBuilder().setV(pb).setK(strkey));
            val v = pbo.getV match {
              case v if v > 0 => v
              case _ => pb.getV
            }
            log.debug("view state:V=" + v);
            Daos.viewstateDB.listBySecondKey(ss.STR_seq(pbo.getTValue) + "." + pb.getOriginBcuid + "." + pb.getMessageUid + "." + v).get match {
              case ovs if ovs != null =>
                ovs.map { x =>
                  //                ret.setNodes(x$1)
                  val ppb = PVBase.newBuilder().mergeFrom(x.getValue.getExtdata);
                  ppb.setContents(ByteString.copyFrom(Base64.encodeBase64(ppb.getContents.toByteArray())))
                  ret.addNodes(NodeStateInfo.newBuilder().setV(ppb).setK(new String(x.getKey.getData.toByteArray())))
                }
            }
          case _ =>
            ret.setRetCode(-1).setRetMessage("NOT FOUND CURR")
        }
      } catch {
        case e: FBSException => {
          ret.clear()
          ret.setRetCode(-2).setRetMessage(e.getMessage)
        }
        case t: Throwable => {
          log.error("error:", t);
          ret.clear()
          ret.setRetCode(-3).setRetMessage(t.getMessage)
        }
      } finally {
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.VTI.name();
}
