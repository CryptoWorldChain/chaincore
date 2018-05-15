package org.brewchain.raftnet

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
import org.brewchain.bcapi.backend.ODBSupport
import org.brewchain.bcapi.backend.ODBDao
import onight.tfw.ojpa.api.StoreServiceProvider
import onight.tfw.ntrans.api.ActorService
import org.brewchain.raftnet.pbgens.Raftnet.PModule
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.core.PZPCtrl
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ojpa.api.IJPAClient

abstract class PSMRaftNet[T <: Message] extends SessionModules[T] with PBUtils with OLog {
  override def getModule: String = PModule.RAF.name()
}

@NActorProvider
@Slf4j
@Provides(specifications = Array(classOf[ActorService],classOf[IJPAClient]))
class Daos extends PSMRaftNet[Message] with ActorService {

  @StoreDAO(target = "bc_bdb", daoClass = classOf[ODSRaftDao])
  @BeanProperty
  var raftdb: ODBSupport = null

  @StoreDAO(target = "bc_bdb", daoClass = classOf[ODSLogIdxDao])
  @BeanProperty
  var idxdb: ODBSupport = null

  def setRaftdb(daodb: DomainDaoSupport) {
    if (daodb != null && daodb.isInstanceOf[ODBSupport]) {
      raftdb = daodb.asInstanceOf[ODBSupport];
      Daos.raftdb = raftdb;
    } else {
      log.warn("cannot set raftdb ODBSupport from:" + daodb);
    }
  }

  def setIdxdb(daodb: DomainDaoSupport) {
    if (daodb != null && daodb.isInstanceOf[ODBSupport]) {
      idxdb = daodb.asInstanceOf[ODBSupport];
      Daos.idxdb = idxdb;
    } else {
      log.warn("cannot set idxdb ODBSupport from:" + daodb);
    }
  }


  @ActorRequire(scope = "global", name = "pzpctrl")
  var pzp: PZPCtrl = null;

  def setPzp(_pzp: PZPCtrl) = {
    pzp = _pzp;
    Daos.pzp = pzp;
  }
  def getPzp(): PZPCtrl = {
    pzp
  }

}

object Daos extends OLog {
  var raftdb: ODBSupport = null
  var idxdb: ODBSupport = null
  var pzp: PZPCtrl = null;
  
  def isDbReady(): Boolean = {
    return raftdb != null && raftdb.getDaosupport.isInstanceOf[ODBSupport] &&
      idxdb != null && idxdb.getDaosupport.isInstanceOf[ODBSupport] &&
      pzp != null;
  }
}



