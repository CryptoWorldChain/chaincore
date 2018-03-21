package org.fc.brewchain.p22p.node

import onight.oapi.scala.traits.OLog
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.Set
import java.math.BigInteger
import org.fc.brewchain.p22p.exception.NodeInfoDuplicated
import onight.tfw.mservice.NodeHelper
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.Message
import java.util.concurrent.atomic.AtomicBoolean
import org.fc.brewchain.p22p.stat.MessageCounter
import org.apache.commons.lang3.StringUtils
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import org.fc.brewchain.p22p.stat.MessageCounter.CCSet
import org.fc.brewchain.p22p.node.router.RandomNR
import onight.tfw.otransio.api.beans.FramePacket

import scala.concurrent.blocking
import java.net.URL

sealed trait Node

case class NoneNode() extends Node

case class PNode(name: String, node_idx: Int, //node info
    protocol: String = "", address: String = "", port: Int = 0, //
    startup_time: Long = System.currentTimeMillis(), //
    pub_key: String = null, //
    counter: CCSet = CCSet(),
    try_node_idx: Int = 0) extends Node with OLog {

  def uri(): String = protocol + "://" + address + ":" + port;

  def processMessage(msg: FramePacket, from: PNode): Unit = {
//    counter.recv.incrementAndGet()
  }

  override def toString(): String = {
    "PNode(" + uri + "," + startup_time + "," + pub_key + "," + node_idx + ")@" + this.hashCode()
  }

  def changeIdx(idx: Int): PNode = PNode(name, idx, protocol, address, port, startup_time, pub_key, counter,idx)
}

object PNode {
  def fromURL(url: String): PNode = {
    val u = new URL(url);
    val n = new PNode(name = u.getHost, node_idx = 0, protocol = u.getProtocol, address = u.getHost, port = u.getPort)
    n
  }
}




