package org.fc.brewchain.p22p.node.router

import onight.oapi.scala.traits.OLog
import scala.beans.BeanProperty

object DHTConsRand extends OLog {
  
  @BeanProperty
  var randFactor = Math.random();

}
