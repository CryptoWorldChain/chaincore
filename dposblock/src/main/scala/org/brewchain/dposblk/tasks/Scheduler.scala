package org.brewchain.dposblk.tasks

import onight.oapi.scala.traits.OLog
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.HashMap
import java.util.concurrent.TimeUnit

import java.util.concurrent.ScheduledFuture

object Scheduler extends OLog {
  val scheduler = new ScheduledThreadPoolExecutor(100);
  def shutdown() {
    scheduler.shutdown()
  }

  val runnerByGroupAddr = new HashMap[String, HashMap[String, ScheduledFuture[_]]]();

  def scheduleWithFixedDelay(command: Runnable,
    initialDelay: Long,
    delay: Long,
    unit: TimeUnit): ScheduledFuture[_] = {
    scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  def stopGroupRunners(group: String) {
    log.debug("stopGroupRunners:" + group);
    runnerByGroupAddr.synchronized({
      val group_runners = runnerByGroupAddr.get(group)
      if (group_runners != null) {
        val it = group_runners.values().iterator();
        while (it.hasNext()) {
          it.next().cancel(true);
        }
        runnerByGroupAddr.remove(group);
      }
    })
  }
  def runOnce(runner: Runnable): Unit = {
    scheduler.submit(runner);
  }

  def updateRunner(group: String, addr: String, runner: Runnable, delay: Long) = {
    runnerByGroupAddr.synchronized({
      var group_runners = runnerByGroupAddr.get(group)
      if (group_runners == null) {
        group_runners = new HashMap[String, ScheduledFuture[_]]();
        runnerByGroupAddr.put(group, group_runners)
      }
      val sf = group_runners.get(addr);
      if (sf != null) {
        sf.cancel(true);
      }
      if (delay > 0) {
        val sf1 = Scheduler.scheduleWithFixedDelay(runner, 0, delay, TimeUnit.SECONDS)
        group_runners.put(addr, sf1);
      } else {
        log.debug("Stop scheduler:" + sf);
      }

    })
  }
  def main(args: Array[String]): Unit = {
    Scheduler.updateRunner("grp1", "abc", new Runnable {
      def run() = {
        println("running::" + System.currentTimeMillis())
      }
    }, 1);

    Thread.sleep(10000);
    Scheduler.updateRunner("grp1", "abc", null, -1);
    Thread.sleep(10000);
    Scheduler.shutdown()
  }
}