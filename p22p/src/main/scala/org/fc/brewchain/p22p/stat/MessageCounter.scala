package org.fc.brewchain.p22p.stat

import java.util.concurrent.atomic.AtomicLong

object MessageCounter {

  val MsgCC = CCSet();
  
  case class CCSet(
    recv: AtomicLong = new AtomicLong(0),
    send: AtomicLong = new AtomicLong(0))

}