package org.brewchain.dposblk.utils

import onight.tfw.mservice.NodeHelper
import onight.tfw.outils.conf.PropHelper

object DConfig {
  val prop: PropHelper = new PropHelper(null);
  val PROP_DOMAIN = "org.bc.dpos."

  val BLK_EPOCH_SEC = prop.get(PROP_DOMAIN + "blk.epoch.sec", 1); //2 seconds each block 
  val MAX_WAIT_BLK_EPOCH_MS = prop.get(PROP_DOMAIN + "max.wait.blk.epoch.ms", 10*1000); //1 min to wait for next block mine 

  val SYNCBLK_PAGE_SIZE = prop.get(PROP_DOMAIN + "syncblk.page.size", 10);

  val VOTE_QUORUM_RATIO = prop.get(PROP_DOMAIN + "vote.quorum.ratio", 60); //60%

  val SYNCBLK_MAX_RUNNER = prop.get(PROP_DOMAIN + "syncblk.max.runner", 10);
  val SYNCBLK_WAITSEC_NEXTRUN = prop.get(PROP_DOMAIN + "syncblk.waitsec.nextrun", 10);
  val SYNCBLK_WAITSEC_ALLRUN = prop.get(PROP_DOMAIN + "syncblk.waitsec.allrun", 600);

  val MAX_BLK_COUNT_PERTERM = prop.get(PROP_DOMAIN + "max.blk.count.perterm", 60);
  val MIN_BLK_COUNT_PERTERM = prop.get(PROP_DOMAIN + "min.blk.count.perterm", 30);

  val MAX_VOTE_WAIT_SEC = prop.get(PROP_DOMAIN + "max.vote.wait.sec", 10);

  val TICK_BLKCTRL_SEC = prop.get(PROP_DOMAIN + "tick.blkctrl.sec", 1);
  val INITDELAY_BLKCTRL_SEC = prop.get(PROP_DOMAIN + "initdelay.blkctrl.sec", 1);

  val DTV_BEFORE_BLK = prop.get(PROP_DOMAIN + "dtv.before.blk", 5);

  val DTV_TIMEOUT_SEC = prop.get(PROP_DOMAIN + "dtv.timeout.sec", -10);
  
  val DTV_MUL_BLOCKS_EACH_TERM = prop.get(PROP_DOMAIN + "dtv.mul.blocks.each.term", 12);
  val DTV_MAX_SUPER_MINER = prop.get(PROP_DOMAIN + "dtv.max.super.miner", 31);
  val DTV_MIN_SUPER_MINER = prop.get(PROP_DOMAIN + "dtv.min.super.miner", 5);
  val DTV_TIME_MS_EACH_BLOCK = prop.get(PROP_DOMAIN + "dtv.time.ms.each_block", 100);
  
  val TICK_DCTRL_SEC = prop.get(PROP_DOMAIN + "tick.dctrl.sec", 1);
  val INITDELAY_DCTRL_SEC = prop.get(PROP_DOMAIN + "initdelay.dctrl.sec", 1);
  
  val BLOCK_DISTANCE_COMINE = prop.get(PROP_DOMAIN + "block.distance.comine", 5);

  val BAN_MINSEC_FOR_VOTE_REJECT = prop.get(PROP_DOMAIN + "ban.minsec.for.vote.reject", 10);
  val BAN_MAXSEC_FOR_VOTE_REJECT = prop.get(PROP_DOMAIN + "ban.maxsec.for.vote.reject", 60);
  
  val MAX_TIMEOUTSEC_FOR_REVOTE = prop.get(PROP_DOMAIN + "max.timeoutsec.for.revote", 30);

}