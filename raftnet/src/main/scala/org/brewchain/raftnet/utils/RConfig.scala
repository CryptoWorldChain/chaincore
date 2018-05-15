package org.brewchain.raftnet.utils

import onight.tfw.mservice.NodeHelper
import onight.tfw.outils.conf.PropHelper

object RConfig {
  val prop: PropHelper = new PropHelper(null);
  val PROP_DOMAIN = "org.bc.rn."

  val VOTE_QUORUM_RATIO = prop.get(PROP_DOMAIN + "vote.quorum.ratio", 60); //60%

  val SYNCLOG_PAGE_SIZE = prop.get(PROP_DOMAIN + "synclog.page.size", 10); //60%

  val SYNCLOG_MAX_RUNNER = prop.get(PROP_DOMAIN + "synclog.max.runner", 10); //60%
  val SYNCLOG_WAITSEC_NEXTRUN = prop.get(PROP_DOMAIN + "synclog.waitsec.nextrun", 10); //60%
  val SYNCLOG_WAITSEC_ALLRUN = prop.get(PROP_DOMAIN + "synclog.waitsec.allrun", 600); //60%

  val CANDIDATE_MAX_WAITMS = prop.get(PROP_DOMAIN + "candidate.max.waitms", 10 * 1000); //60%
  val CANDIDATE_MIN_WAITMS = prop.get(PROP_DOMAIN + "candidate.min.waitms", 100); //60%

  val MAX_TERM_SEC = prop.get(PROP_DOMAIN + "max.term.sec", 60); //60%
  val MIN_TERM_SEC = prop.get(PROP_DOMAIN + "min.term.sec", 10); //60%
  
  val MAX_VOTE_WAIT_SEC = prop.get(PROP_DOMAIN + "max.vote.wait.sec", 10); //60%
  
  val COMMIT_LOG_BATCH = prop.get(PROP_DOMAIN + "commit.log.batch", 10);
  val COMMIT_LOG_TIMEOUT_SEC = prop.get(PROP_DOMAIN + "commit.log.timeout.sec", 60);

  val TICK_RSM_SEC = prop.get(PROP_DOMAIN + "tick.rsm.sec", 1);
  val INITDELAY_RSM_SEC = prop.get(PROP_DOMAIN + "initdelay.rsm.sec", 1);

}