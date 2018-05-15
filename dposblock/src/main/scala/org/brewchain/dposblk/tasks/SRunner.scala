package org.brewchain.dposblk.tasks

import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.action.PMNodeHelper

trait SRunner extends Runnable with OLog with PMNodeHelper {

  def runOnce()

  def getName(): String

  def run() = {
    val oldname = Thread.currentThread().getName;
    Thread.currentThread().setName(getName());
//          log.debug(getName() + ": ----------- [START]")
    try {
      runOnce()
    } catch {
      case e: Throwable =>
//        log.debug(getName() + ":  ----------- Error", e);
    } finally {
//      log.debug(getName() + ":  ----------- [END]")
      Thread.currentThread().setName(oldname + "");
    }
  }
}