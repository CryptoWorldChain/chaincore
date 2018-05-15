package org.brewchain.dposblk

import onight.osgi.annotation.NActorProvider
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import org.apache.felix.ipojo.annotations.Validate
import org.apache.felix.ipojo.annotations.Invalidate
import org.fc.brewchain.bcapi.URLHelper
import onight.tfw.otransio.api.NonePackSender
import onight.oapi.scala.traits.OLog
import java.net.URL
import onight.tfw.mservice.NodeHelper
import java.util.concurrent.TimeUnit
import org.brewchain.dposblk.tasks.DCtrl
import org.brewchain.dposblk.tasks.DPosNodeController
import org.brewchain.dposblk.utils.DConfig
import org.brewchain.dposblk.tasks.Scheduler

@NActorProvider
class DPoSStartup extends PSMDPoSNet[Message] {

  override def getCmds: Array[String] = Array("SSS");

  @Validate
  def init() {

    //    System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.url");
    log.debug("startup:");
    new Thread(new DPoSBGLoader()).start()

    log.debug("tasks inited....[OK]");
  }

  @Invalidate
  def destory() {

  }

}

class DPoSBGLoader() extends Runnable with OLog {
  def run() = {
    URLHelper.init();
    while (!Daos.isDbReady() //        || MessageSender.sockSender.isInstanceOf[NonePackSender]
    ) {
      log.debug("Daos Or sockSender Not Ready..:pzp=" + Daos.pzp)
      Thread.sleep(1000);
    }

    log.debug("Ready to Start..:pzp=" + Daos.pzp + ",dposdb=" + Daos.dposdb)

    var dposnet = Daos.pzp.networkByID("dpos")

    while (dposnet == null
      || dposnet.node_bits().bitCount <= 0 || !dposnet.inNetwork()) {
      dposnet = Daos.pzp.networkByID("dpos")
      log.debug("dposnet not ready. dposnet=" + dposnet)
      Thread.sleep(5000);
    }
    log.debug("dposnet.initOK:My Node" + dposnet.root()) // my node
//    RSM.instance = RaftStateManager(raftnet);

    DCtrl.instance = DPosNodeController(dposnet);
    Scheduler.scheduleWithFixedDelay(DCtrl.instance, DConfig.INITDELAY_DCTRL_SEC,
      DConfig.TICK_DCTRL_SEC, TimeUnit.SECONDS)

//    Scheduler.scheduleWithFixedDelay(RSM.instance, RConfig.INITDELAY_RSM_SEC,
//      RConfig.TICK_RSM_SEC, TimeUnit.SECONDS)


  }
}