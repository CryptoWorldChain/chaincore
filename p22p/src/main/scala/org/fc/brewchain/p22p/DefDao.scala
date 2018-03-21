package org.fc.brewchain.p22p

import scala.beans.BeanProperty

import com.google.protobuf.Message

import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.ojpa.api.DomainDaoSupport
import onight.tfw.ojpa.api.annotations.StoreDAO
import onight.tfw.oparam.api.OParam
import onight.tfw.otransio.api.IPacketSender
import onight.tfw.otransio.api.PSender
import onight.tfw.ntrans.api.annotation.ActorRequire
import org.fc.brewchain.p22p.pbgens.P22P.PModule

abstract class PSMPZP[T <: Message] extends SessionModules[T] with PBUtils with OLog {
  override def getModule: String = PModule.PZP.name()
}

@NActorProvider
@Slf4j
object Daos extends PSMPZP[Message] {
  @StoreDAO(target = "etcd", daoClass = classOf[OParam])
  @BeanProperty
  var oparam: OParam = null

  @StoreDAO(target = "obdb", daoClass = classOf[OParam])
  @BeanProperty
  var odb: OParam = null

  def setOparam(daoparam: DomainDaoSupport) {
    if (daoparam != null && daoparam.isInstanceOf[OParam]) {
      oparam = daoparam.asInstanceOf[OParam];
    } else {
      log.warn("cannot set OParam from:" + daoparam);
    }
  }
  
  def setOdb(daoparam: DomainDaoSupport) {
    if (daoparam != null && daoparam.isInstanceOf[OParam]) {
      odb = daoparam.asInstanceOf[OParam];
    } else {
      log.warn("cannot set OParam from:" + daoparam);
    }
  }

  @BeanProperty
  @PSender
  var pSender: IPacketSender = null;

  @BeanProperty
  @ActorRequire(name = "http", scope = "global")
  var httpsender: IPacketSender = null;

}


