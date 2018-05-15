package org.fc.brewchain.p22p

import onight.osgi.annotation.NActorProvider
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import org.apache.felix.ipojo.annotations.Validate
import org.apache.felix.ipojo.annotations.Invalidate
import org.fc.brewchain.bcapi.URLHelper
import org.fc.brewchain.p22p.node.Networks
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.core.MessageSender
import onight.tfw.otransio.api.NonePackSender
import onight.oapi.scala.traits.OLog
import java.net.URL
import onight.tfw.mservice.NodeHelper
import org.fc.brewchain.p22p.node.ClusterNode
import scala.collection.JavaConversions._
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.node.PNode
import java.util.HashMap

@NActorProvider
class Startup extends PSMPZP[Message] {

  override def getCmds: Array[String] = Array("SSS");

  @Validate
  def init() {

    //    System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.url");
    log.debug("startup:");
    new Thread(new PZPBGLoader()).start()

    log.debug("tasks inited....[OK]");
  }

  @Invalidate
  def destory() {

  }

}

class PZPBGLoader() extends Runnable with OLog {
  def run() = {
    URLHelper.init();
    while (!Daos.isDbReady() || MessageSender.sockSender.isInstanceOf[NonePackSender]) {
      log.debug("Daos Or sockSender Not Ready..:enc=" + Daos.enc + ",sender=" + MessageSender.sockSender
        + ",daoready=" + Daos.isDbReady())
      Thread.sleep(1000);
    }

    val networks = Daos.props.get("org.bc.pzp.networks", "raft").split(",").toList
    log.debug("networks:" + networks)
    log.debug("enc:" + Daos.enc)
    // for test..
    // create two layer networks , [1,3,5,7] ==> [2,4,6,8]
    // layer one , like raft
    networks.map { x =>
      val net = new Network(x.trim(), Daos.props.get("org.bc.pzp.networks." + x.trim() + ".nodelist",
        "tcp://127.0.0.1:510" + NodeHelper.getCurrNodeListenOutPort % 2));
      Networks.netsByID.put(net.netid, net)
    }
    val initlist = new HashMap[String, Network]();
    initlist.putAll(Networks.netsByID);
    while (initlist.size() > 0) {
      Networks.netsByID.filter(p => initlist.containsKey(p._1)).map { f =>
        val net = f._2;
        val subnet = Networks.networkByID(Daos.props.get("org.bc.pzp.networks." + net.netid + ".subnet", ""))
        if (subnet != null) {
          if (subnet.root() != PNode.NoneNode) {
            log.debug("cluster ready: " + net.netid + " for startup:" + f._1 + "  ... [OK]");
            net.initClusterNode(subnet.root());
            net.startup()
            initlist.remove(f._1)
          } else {
            log.debug("subnet not ready: " + net.netid + " for cluster:" + f._1 + " ... [OK]");
          }
        } else {
          log.debug("init net: " + net.netid + " ... [OK]");
          net.initNode();
          net.startup()
          initlist.remove(f._1)
        }
      }
      Thread.sleep(1000);
    }

    log.debug("All Network Startup!" + networks)
    //    net.initNode();
    //    net.startup()

    //layer two, like dpos
    //    if (Daos.props.get("org.bc.pzp.networks.test", "0").equals("1") || Daos.props.get("org.bc.pzp.networks.test", "false").equals("true")) {
    //      val net0 = Networks.networkByID("raft");
    //      {
    //        val net1 = new Network("dpos", "tcp://127.0.0.1:5100");
    //        Networks.netsByID.put(net1.netid, net1)
    //        net1.initClusterNode(net0.root());
    //        net1.startup()
    //      }
    //    }

  }
}