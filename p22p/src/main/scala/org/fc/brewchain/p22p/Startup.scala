package org.fc.brewchain.p22p

import onight.osgi.annotation.NActorProvider
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import org.apache.felix.ipojo.annotations.Validate
import org.apache.felix.ipojo.annotations.Invalidate
import org.fc.brewchain.p22p.tasks.LayerNodeTask
import org.fc.brewchain.p22p.node.NodeInstance
import org.fc.brewchain.bcapi.URLHelper

@NActorProvider
object Startup extends SessionModules[Message] {

  @Validate
  def init() {
    System.setProperty("java.protocol.handler.pkgs", "org.fc.brewchain.url");

    log.info("startup:");

    LayerNodeTask.initTask();
    
    log.info("tasks inited....[OK]");
  }

  @Invalidate
  def destory() {

  }
}