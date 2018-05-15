package org.brewchain.dposblk

import scala.beans.BeanProperty

import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.traits.OLog
import org.brewchain.dposblk.pbgens.Dposblock.PModule
import onight.osgi.annotation.NActorProvider
import onight.tfw.ntrans.api.ActorService
import onight.tfw.ojpa.api.IJPAClient
import onight.tfw.ojpa.api.annotations.StoreDAO
import org.brewchain.bcapi.backend.ODBSupport
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ojpa.api.DomainDaoSupport
import onight.tfw.ntrans.api.annotation.ActorRequire
import org.fc.brewchain.p22p.core.PZPCtrl

abstract class PSMDPoSNet[T <: Message] extends SessionModules[T] with PBUtils with OLog {
  override def getModule: String = PModule.DOB.name()
}

@NActorProvider
@Provides(specifications = Array(classOf[ActorService],classOf[IJPAClient]))
class Daos extends PSMDPoSNet[Message] with ActorService {

  @StoreDAO(target = "bc_bdb", daoClass = classOf[ODSDPoSDao])
  @BeanProperty
  var dposdb: ODBSupport = null

  @StoreDAO(target = "bc_bdb", daoClass = classOf[ODSBlkDao])
  @BeanProperty
  var blkdb: ODBSupport = null

  def setDposdb(daodb: DomainDaoSupport) {
    if (daodb != null && daodb.isInstanceOf[ODBSupport]) {
      dposdb = daodb.asInstanceOf[ODBSupport];
      Daos.dposdb = dposdb;
    } else {
      log.warn("cannot set dposdb ODBSupport from:" + daodb);
    }
  }

  def setBlkdb(daodb: DomainDaoSupport) {
    if (daodb != null && daodb.isInstanceOf[ODBSupport]) {
      blkdb = daodb.asInstanceOf[ODBSupport];
      Daos.blkdb = blkdb;
    } else {
      log.warn("cannot set blkdb ODBSupport from:" + daodb);
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
  var dposdb: ODBSupport = null
  var blkdb: ODBSupport = null
  var pzp: PZPCtrl = null;
  
  def isDbReady(): Boolean = {
    return dposdb != null && dposdb.getDaosupport.isInstanceOf[ODBSupport] &&
      blkdb != null && blkdb.getDaosupport.isInstanceOf[ODBSupport] &&
      pzp != null;
  }
}



