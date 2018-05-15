package org.brewchain.dposblk.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import onight.tfw.outils.serialize.UUIDGenerator
import scala.collection.JavaConversions._
import org.fc.brewchain.p22p.core.Votes
import org.fc.brewchain.p22p.core.Votes.Converge
import org.fc.brewchain.p22p.core.Votes.Undecisible
import org.brewchain.dposblk.utils.DConfig
import org.fc.brewchain.bcapi.JodaTimeHelper
import org.brewchain.dposblk.Daos
import org.brewchain.dposblk.pbgens.Dposblock.PDutyTermResult
import scala.collection.mutable.Buffer
import org.brewchain.dposblk.pbgens.Dposblock.PDutyTermResult.VoteResult
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVote
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVote.BlockRange
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVote.TermBlock
import org.apache.commons.lang3.StringUtils

//获取其他节点的term和logidx，commitidx
object DTask_DutyTermVote extends LogHelper {
  def checkVoteDB(vq: PSDutyTermVote.Builder)(implicit network: Network): Boolean = {
    val records = Daos.dposdb.listBySecondKey("D" + vq.getTermId + "-" + vq.getSign)
    log.debug("check db status:B[=" + vq.getBlockRange.getStartBlock + ","
      + vq.getBlockRange.getEndBlock + "],T="
      + vq.getTermId
      + ",sign=" + vq.getSign
      + ",N=" + vq.getCoNodes
      + ",dbsize=" +
      records.get.size())
    if ((records.get.size() + 1) >= DCtrl.termMiner().getCoNodes * DConfig.VOTE_QUORUM_RATIO / 100) {
      log.debug("try to vote:" + records.get.size());
      val reclist: Buffer[PDutyTermResult.Builder] = records.get.map { p =>
        PDutyTermResult.newBuilder().mergeFrom(p.getValue.getExtdata);
      };
      val realist = reclist.filter { p => DCtrl.coMinerByUID.containsKey(p.getBcuid) };
      log.debug("check db status:B[=" + vq.getBlockRange.getStartBlock + ","
        + vq.getBlockRange.getEndBlock + "],T="
        + vq.getTermId
        + ",sign=" + vq.getSign
        + ",N=" + vq.getCoNodes
        + ",dbsize=" + records.get.size()
        + ",realsize=" + realist.size())
      Votes.vote(realist).PBFTVote({ p =>
        Some(p.getResult)
      }, vq.getCoNodes) match {
        case Converge(n) =>
          log.debug("converge:" + n);
          if (n == VoteResult.VR_GRANTED) {
            log.debug("Vote Granted will be the new terms:" + vq);
            DCtrl.instance.term_Miner = vq.clone()
            DCtrl.instance.updateTerm()
            true
          } else if (n == VoteResult.VR_REJECT) {
            val ban_sec = (Math.abs(Math.random() * 100000 % (DConfig.BAN_MAXSEC_FOR_VOTE_REJECT - DConfig.BAN_MINSEC_FOR_VOTE_REJECT)) +
              DConfig.BAN_MINSEC_FOR_VOTE_REJECT).asInstanceOf[Long]
            log.debug("Vote reject:" + n + ",ban and sleep:" + ban_sec);
            Daos.dposdb.batchDelete(records.get.map { p => p.getKey }.toArray)
            vq.clear();
            Thread.sleep(ban_sec * 1000)
            //              RSM.resetVoteRequest();
            false
          } else {
            val ban_sec = (Math.abs(Math.random() * 100000 % (DConfig.BAN_MAXSEC_FOR_VOTE_REJECT - DConfig.BAN_MINSEC_FOR_VOTE_REJECT)) +
              DConfig.BAN_MINSEC_FOR_VOTE_REJECT).asInstanceOf[Long]
            log.debug("unknow vote state:" + n + ",ban and sleep:" + ban_sec)
            Daos.dposdb.batchDelete(records.get.map { p => p.getKey }.toArray)
            vq.clear();
            Thread.sleep(ban_sec * 1000)
            //              RSM.resetVoteRequest();
            false
          }
        case n: Undecisible =>
          if (records.get.size() == DCtrl.termMiner().getCoNodes - 1) {
            val ban_sec = (Math.abs(Math.random() * 100000 % (DConfig.BAN_MAXSEC_FOR_VOTE_REJECT - DConfig.BAN_MINSEC_FOR_VOTE_REJECT)) +
              DConfig.BAN_MINSEC_FOR_VOTE_REJECT).asInstanceOf[Long]
            log.debug("Undecisible but not converge.ban sleep=" + ban_sec)
            if (System.currentTimeMillis() - vq.getTermStartMs > DConfig.MAX_TIMEOUTSEC_FOR_REVOTE * 1000) {
              log.debug("remove undecisible vote for timeout:"+(System.currentTimeMillis() - vq.getTermStartMs));
              Daos.dposdb.batchDelete(records.get.map { p => p.getKey }.toArray)
              vq.clear();
            }
            Thread.sleep(ban_sec * 1000)

            //          !!    RSM.resetVoteRequest();
          } else {
            log.debug("cannot decide vote state, wait other response")
          }
          false
        case a @ _ =>
          log.debug("not converge,try next time:::" + a)
          //            RSM.resetVoteRequest();
          false
      }
    } else {
      log.debug("check status Not enough results:B[=" + vq.getBlockRange.getStartBlock + ","
        + vq.getBlockRange.getEndBlock + "],T="
        + vq.getTermId
        + ",sign=" + vq.getSign
        + ",N=" + vq.getCoNodes
        + ",dbsize=" + records.get.size())
      false
    }
  }
  def runOnce(implicit network: Network): Boolean = {
    Thread.currentThread().setName("RTask_RequestVote");
    val cn = DCtrl.curDN();
    val tm = DCtrl.termMiner();
    val vq = DCtrl.voteRequest()
    if (cn.getCurBlock + DConfig.DTV_BEFORE_BLK >= tm.getBlockRange.getEndBlock
      && vq.getBlockRange.getStartBlock >= tm.getBlockRange.getEndBlock
      && vq.getBlockRange.getStartBlock >= cn.getCurBlock
      && vq.getTermId > 0
      || (StringUtils.isNotBlank(vq.getLastTermUid) && vq.getLastTermUid.equals(tm.getMessageId))) {
      checkVoteDB(vq)
    } else if (cn.getCurBlock + DConfig.DTV_BEFORE_BLK >= tm.getBlockRange.getEndBlock
      || JodaTimeHelper.secondIntFromNow(tm.getTermEndMs) > DConfig.DTV_TIMEOUT_SEC) {

      val msgid = UUIDGenerator.generate();
      MDCSetMessageID(msgid);

      //      log.debug("try vote new term:");
      DCtrl.coMinerByUID.filter(p => {
        network.nodeByBcuid(p._1) == network.noneNode
      }).map { p =>
        log.debug("remove Node:" + p._1);
        DCtrl.coMinerByUID.remove(p._1);
      }

      val newterm = PSDutyTermVote.newBuilder();
      val conodescount = Math.min(DCtrl.coMinerByUID.size, DConfig.DTV_MAX_SUPER_MINER);
      val mineBlockCount = DConfig.DTV_MUL_BLOCKS_EACH_TERM * conodescount;
      val startBlk = cn.getCurBlock + 1;
      newterm.setBlockRange(BlockRange.newBuilder()
        .setStartBlock(startBlk)
        .setEndBlock(startBlk + mineBlockCount - 1)
        .setEachBlockSec(DConfig.BLK_EPOCH_SEC))
        .setCoNodes(conodescount)
        .setMessageId(msgid)
        .setCoAddress(DCtrl.instance.cur_dnode.getCoAddress)
        .setCwsGuaranty(1)
        .setSliceId(1)
        .setTermStartMs(System.currentTimeMillis());
      newterm.setTermEndMs(DConfig.DTV_TIME_MS_EACH_BLOCK * mineBlockCount);

      newterm.setTermId(tm.getTermId + 1)
        .setLastTermId(tm.getTermId)
        .setLastTermUid(tm.getMessageId)
        .setSign(msgid)

      val rand = Math.random() * 1000
      val rdns = scala.util.Random.shuffle(DCtrl.coMinerByUID);
      log.debug(" rdns=" + rdns.foldLeft("")((a, b) => a + "," + b._1));
      var i = newterm.getBlockRange.getStartBlock;
      var bitcc = BigInt(0);

      while (newterm.getMinerQueueCount < mineBlockCount) {
        rdns.map { x =>
          if (newterm.getMinerQueueCount < mineBlockCount) {
            log.debug(" add miner at Queue," + x._2.getCoAddress + ",blockheight=" + i);
            newterm.addMinerQueue(TermBlock.newBuilder().setBlockHeight(i)
              .setMinerCoaddr(x._2.getCoAddress))
            i = i + 1;
          }
        }
      }
      log.debug("mineQ=" + newterm.getMinerQueueList)

      log.debug("get coMinerNodeCount=" + DCtrl.coMinerByUID.size + ",NetworkDNodecount=" + network.directNodeByBcuid.size);

      //checking health remove offline nodes.

      val curtime = System.currentTimeMillis()
      DCtrl.instance.vote_Request = newterm;
      log.debug("try to vote:newterm=" + newterm.getTermId + ",curterm=" + tm.getTermId
        + ",voteN=" + conodescount + ",sign=" + newterm.getSign)
      network.dwallMessage("DTVDOB", Left(DCtrl.voteRequest().build()), msgid);
      false
    } else {
      false
    }
  }

}
