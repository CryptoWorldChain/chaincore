package org.brewchain.raftnet

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
import org.brewchain.raftnet.tasks.RaftStateManager
import org.brewchain.raftnet.tasks.RSM
import org.brewchain.raftnet.tasks.Scheduler
import java.util.concurrent.TimeUnit
import org.brewchain.raftnet.utils.RConfig

@NActorProvider
class RFTStartup extends PSMRaftNet[Message] {

  override def getCmds: Array[String] = Array("SSS");

  @Validate
  def init() {

    //    System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.url");
    log.debug("startup:");
    new Thread(new RaftBGLoader()).start()

    log.debug("tasks inited....[OK]");
  }

  @Invalidate
  def destory() {

  }

}

class RaftBGLoader() extends Runnable with OLog {
  def run() = {
    URLHelper.init();
    while (!Daos.isDbReady() //        || MessageSender.sockSender.isInstanceOf[NonePackSender]
    ) {
      log.debug("Daos Or sockSender Not Ready..:pzp=" + Daos.pzp + ",raftdb=" + Daos.raftdb)
      Thread.sleep(1000);
    }

    log.debug("Ready to Start..:pzp=" + Daos.pzp + ",raftdb=" + Daos.raftdb)

    var raftnet = Daos.pzp.networkByID("raft")

    while (raftnet == null
      || raftnet.node_bits().bitCount <= 0 || !raftnet.inNetwork()) {
      raftnet = Daos.pzp.networkByID("raft")
      log.debug("raftnet not ready. raftnet=" + raftnet)
      Thread.sleep(5000);
    }
    log.debug("raftnet.initOK:My Node" + raftnet.root()) // my node
    RSM.instance = RaftStateManager(raftnet);

    Scheduler.scheduleWithFixedDelay(RSM.instance, RConfig.INITDELAY_RSM_SEC,
      RConfig.TICK_RSM_SEC, TimeUnit.SECONDS)


  }
}