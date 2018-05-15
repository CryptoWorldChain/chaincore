package org.fc.brewchain.p22p.utils

import onight.tfw.mservice.NodeHelper
import onight.tfw.outils.conf.PropHelper

object Config {
  val prop: PropHelper = new PropHelper(null);
  val PROP_DOMAIN = "org.bc.pzp."
  val TIMEOUT_STATE_VIEW = prop.get(PROP_DOMAIN + "timeout.state.view", 60 * 1000);
  val TIMEOUT_STATE_VIEW_RESET = prop.get(PROP_DOMAIN + "timeout.state.view.reset", 360 * 1000);
  val MIN_EPOCH_EACH_VOTE = prop.get(PROP_DOMAIN + "min.epoch.each.vote", 10*1000)
  val MAX_VOTE_SLEEP_MS = prop.get(PROP_DOMAIN + "max.vote.sleep.ms", 60000);
  val BAN_FOR_VOTE_SLEEP_MS = prop.get(PROP_DOMAIN + "bank.for.vote.sleep.ms", 60000);
  def getRandSleepForBan(): Int = {
    (Math.random() * BAN_FOR_VOTE_SLEEP_MS + BAN_FOR_VOTE_SLEEP_MS*2).asInstanceOf[Int];
  }
  val MIN_VOTE_SLEEP_MS = prop.get(PROP_DOMAIN + "min.vote.sleep.ms", 10000);

  val MIN_VOTE_WITH_NOCHANGE_SLEEP_MS = prop.get(PROP_DOMAIN + "min.vote.sleep.nochange.ms", 120 * 1000);

  val TICK_CHECK_HEALTHY = prop.get(PROP_DOMAIN + "tick.check.healthy", 10);
  val TICK_JOIN_NETWORK = prop.get(PROP_DOMAIN + "tick.join.network", 60);
  val TICK_VOTE_MAP = prop.get(PROP_DOMAIN + "tick.vote.map", 10);
  val TICK_VOTE_WORKER = prop.get(PROP_DOMAIN + "tick.vote.worker", 1);
  val NUM_VIEWS_EACH_SNAPSHOT = prop.get(PROP_DOMAIN + "num.views.each.snapshot", 10); //每快照有几个

  val STR_REJECT = "__REJECT";

  def VOTE_DEBUG: Boolean = {
    //    NodeHelper.getCurrNodeListenOutPort != 5100;
    false
  }
}