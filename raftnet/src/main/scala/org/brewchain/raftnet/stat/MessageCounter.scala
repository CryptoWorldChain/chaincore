package org.brewchain.raftnet.stat

import java.util.concurrent.atomic.AtomicLong

object MessageCounter {

  val MsgCC = CCSet();

  case class CCSet(
      recv: AtomicLong = new AtomicLong(0), 
      send: AtomicLong = new AtomicLong(0),
      blocks: AtomicLong = new AtomicLong(0)) {
    def this(recv: Long, send: Long, blocks: Long) = {
      this(new AtomicLong(recv), new AtomicLong(send), new AtomicLong(blocks))
    }
  }

}