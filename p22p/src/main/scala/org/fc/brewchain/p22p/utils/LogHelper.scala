package org.fc.brewchain.p22p.utils

import org.apache.commons.lang3.StringUtils
import org.slf4j.MDC
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.node.Network

trait LogHelper extends OLog {
  def getAbr(str: String) = StringUtils.abbreviateMiddle(str, ".", 10);
//  def MDCSetBCUID(bcuid: String) = MDC.put("BCUID", getAbr(bcuid));
  
  def MDCSetBCUID(network:Network) = { 
    MDC.put("BCUID",getAbr(network.root().bcuid));
    System.setProperty("LOGROOT",network.root().bcuid);
  } 
  
  def MDCSetBCUID(bcuid:String) = { 
    MDC.put("BCUID",getAbr(bcuid));
    System.setProperty("LOGROOT",bcuid);
  }
  
  def MDCSetMessageID(msgid: String) = MDC.put("MessageID", msgid);
  def MDCRemoveMessageID() = MDC.remove("MessageID");
  
  
}