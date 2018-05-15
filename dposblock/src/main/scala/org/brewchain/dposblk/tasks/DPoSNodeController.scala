package org.brewchain.dposblk.tasks

import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.utils.LogHelper
import org.brewchain.bcapi.gens.Oentity.OValue
import org.apache.commons.lang3.StringUtils
import org.fc.brewchain.p22p.node.Node
import scala.collection.mutable.Map
import java.util.concurrent.atomic.AtomicLong
import org.fc.brewchain.bcapi.JodaTimeHelper
import org.brewchain.dposblk.pbgens.Dposblock.PDNode
import java.util.concurrent.atomic.AtomicInteger
import org.brewchain.dposblk.Daos
import org.brewchain.dposblk.pbgens.Dposblock.DNodeState
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVote
import org.brewchain.dposblk.pbgens.Dposblock.PDNodeOrBuilder
import org.brewchain.dposblk.pbgens.Dposblock.PSDutyTermVoteOrBuilder
import org.brewchain.dposblk.pbgens.Dposblock.PDutyTermResult
import org.brewchain.dposblk.utils.DConfig
import org.brewchain.dposblk.pbgens.Dposblock.PBlockEntry
import org.brewchain.dposblk.pbgens.Dposblock.PBlockEntryOrBuilder

import scala.collection.JavaConversions._
//投票决定当前的节点
case class DPosNodeController(network: Network) extends SRunner with LogHelper {
  def getName() = "DCTRL"
  val DPOS_NODE_DB_KEY = "CURRENT_DPOS_KEY";
  val DPOS_NODE_DB_TERM = "CURRENT_DPOS_TERM";
  var cur_dnode: PDNode.Builder = PDNode.newBuilder()
  var term_Miner: PSDutyTermVote.Builder = PSDutyTermVote.newBuilder();
  var vote_Request: PSDutyTermVote.Builder = PSDutyTermVote.newBuilder();

  val cur_blk_height = new AtomicInteger(1)

  def nextBlock(): Long = {
    cur_blk_height.incrementAndGet()
  }

  def retsetBlockHeight(hi: Int): Unit = {
    cur_blk_height.set(hi)
  }

  def updateVoteReq(pbo: PSDutyTermVote): Unit = {
    vote_Request = pbo.toBuilder()
    cur_dnode.setNodeCount(vote_Request.getCoNodes)
    syncToDB();
  }
  def loadNodeFromDB(): PDNode.Builder = {
    val ov = Daos.dposdb.get(DPOS_NODE_DB_KEY).get
    val root_node = network.root();
    if (ov == null) {
      cur_dnode.setBcuid(root_node.bcuid)
        .setCurBlock(1).setCoAddress(root_node.v_address)
        .setBitIdx(root_node.node_idx)
      Daos.dposdb.put(DPOS_NODE_DB_KEY,
        OValue.newBuilder().setExtdata(cur_dnode.build().toByteString()).build())
    } else {
      cur_dnode.mergeFrom(ov.getExtdata)
      if (!StringUtils.equals(cur_dnode.getBcuid, root_node.bcuid)) {
        log.warn("load from dnode info not equals with pzp node:" + cur_dnode + ",root=" + root_node)
      } else {
        log.info("load from db:OK" + cur_dnode)
      }
    }

    val termov = Daos.dposdb.get(DPOS_NODE_DB_TERM).get
    if (termov == null) {
      Daos.dposdb.put(DPOS_NODE_DB_TERM,
        OValue.newBuilder().setExtdata(term_Miner.build().toByteString()).build())
    } else {
      term_Miner.mergeFrom(termov.getExtdata)
    }
    cur_dnode
  }
  def syncToDB() {
    Daos.dposdb.put(DPOS_NODE_DB_KEY,
      OValue.newBuilder().setExtdata(cur_dnode.build().toByteString()).build())
  }
  def updateTerm() = {
    Daos.dposdb.put(DPOS_NODE_DB_TERM,
      OValue.newBuilder().setExtdata(term_Miner.build().toByteString()).build())
  }
  def updateBlockHeight(blockHeight: Int) = {
    if (cur_dnode.getCurBlock < blockHeight) {
      cur_dnode.setLastBlockTime(System.currentTimeMillis())
      cur_dnode.setCurBlock(blockHeight)
      syncToDB()
    }
  }
  def runOnce() = {
    Thread.currentThread().setName("DCTRL");
    implicit val _net = network
    MDCSetBCUID(network);
    MDCRemoveMessageID()
    var continue = true;
    while (continue) {
      try {
        continue = false;
        log.info("DCTRL.RunOnce:S=" + cur_dnode.getState + ",B=" + cur_dnode.getCurBlock
          + ",CA=" + cur_dnode.getCoAddress
          + ",N=" + cur_dnode.getNodeCount
          + ",RN=" + network.bitenc.bits.bitCount
          + ",TN=" + cur_dnode.getTxcount + ",DU=" + cur_dnode.getDutyUid
          + ",VT=" + vote_Request.getTermId
          + ",TM=" + term_Miner.getTermId
          + ",VU=" + vote_Request.getLastTermUid
          + ",NextSec=" + JodaTimeHelper.secondFromNow(cur_dnode.getDutyEndMs)
          + ",SecPass=" + JodaTimeHelper.secondFromNow(cur_dnode.getLastDutyTime));
        cur_dnode.getState match {
          case DNodeState.DN_INIT =>
            //tell other I will join
            loadNodeFromDB();
            continue = DTask_CoMine.runOnce match {
              case n: PDNode if n == cur_dnode =>
                log.debug("dpos cominer init ok:" + n);
                true;
              case n: PDNode if n != cur_dnode =>
                log.debug("dpos waiting for init:" + n);
                false
              case x @ _ =>
                log.debug("not ready");
                false
            }
          case DNodeState.DN_CO_MINER =>
            if (DTask_DutyTermVote.runOnce) {
              continue = true;
              cur_dnode.setState(DNodeState.DN_DUTY_MINER);
            }
          case DNodeState.DN_DUTY_MINER =>
            if (DTask_MineBlock.runOnce) {
              if (cur_dnode.getCurBlock >= DCtrl.voteRequest().getBlockRange.getEndBlock
                || term_Miner.getTermId < vote_Request.getTermId) {
                log.debug("cur term WILL end:newblk=" + cur_dnode.getCurBlock + ",term[" + DCtrl.voteRequest().getBlockRange.getStartBlock
                  + "," + DCtrl.voteRequest().getBlockRange.getEndBlock + "]" + ",T=" + term_Miner.getTermId);
                continue = true;
                val sleept = Math.abs((Math.random() * DConfig.DTV_TIME_MS_EACH_BLOCK).asInstanceOf[Long]) + 10;
                log.debug("Duty_Miner To CoMiner:sleep=" + sleept)
                cur_dnode.setState(DNodeState.DN_CO_MINER);
                Thread.sleep(sleept);
                true
              } else {
                log.debug("cur term NOT end:newblk=" + cur_dnode.getCurBlock + ",term[" + DCtrl.voteRequest().getBlockRange.getStartBlock
                  + "," + DCtrl.voteRequest().getBlockRange.getEndBlock + "]");
                false
              }
            } else {
              //check who mining.
              if (cur_dnode.getLastBlockTime > 0 && JodaTimeHelper.secondIntFromNow(cur_dnode.getLastBlockTime)
                > DConfig.MAX_WAIT_BLK_EPOCH_MS / 1000) {
                //this block is ban because lost one 
                log.debug("lost Miner Block:B=" + cur_dnode.getCurBlock + ",past=" + JodaTimeHelper.secondIntFromNow(cur_dnode.getLastBlockTime));
              }
              if (cur_dnode.getCurBlock >= DCtrl.voteRequest().getBlockRange.getEndBlock) {
                continue = true;
                val sleept = Math.abs((Math.random() * DConfig.DTV_TIME_MS_EACH_BLOCK).asInstanceOf[Long]) + 10;
                log.debug("Duty_Miner To CoMiner:sleep=" + sleept)
                cur_dnode.setState(DNodeState.DN_CO_MINER);
                Thread.sleep(sleept);
                true
              } else {
                false
              }
            }
          case DNodeState.DN_SYNC_BLOCK =>
            DTask_CoMine.runOnce
          case DNodeState.DN_BAKCUP =>
            DTask_CoMine.runOnce
          case _ =>
            log.warn("unknow State:" + cur_dnode.getState);

        }

      } catch {
        case e: Throwable =>
          log.warn("dpos control :Error", e);
      } finally {
        MDCRemoveMessageID()
      }
    }
  }
}

object DCtrl extends LogHelper {
  var instance: DPosNodeController = DPosNodeController(null);
  def dposNet(): Network = instance.network;
  //  val superMinerByUID: Map[String, PDNode] = Map.empty[String, PDNode];
  val coMinerByUID: Map[String, PDNode] = Map.empty[String, PDNode];
  def curDN(): PDNode.Builder = instance.cur_dnode
  def termMiner(): PSDutyTermVoteOrBuilder = instance.term_Miner
  def voteRequest(): PSDutyTermVote.Builder = instance.vote_Request

  //  def curTermMiner(): PSDutyTermVoteOrBuilder = instance.term_Miner

  def isReady(): Boolean = {
    instance.network != null &&
      instance.cur_dnode.getStateValue > DNodeState.DN_INIT_VALUE
  }

  def checkMiner(block: Int, coaddr: String, mineTime: Long, maxWaitMS: Long = 1L): Boolean = {
    val vr = voteRequest().getBlockRange;
    val blkshouldMineMS = (block - vr.getStartBlock + 1) * vr.getEachBlockSec * 1000 + voteRequest().getTermStartMs
    val realblkMineMS = mineTime;
    val termblockLeft = block - vr.getEndBlock
    minerByBlockHeight(block) match {
      case Some(n) =>
        if (coaddr.equals(n)) {
          if (realblkMineMS < blkshouldMineMS) {
            log.debug("wait for time to Mine:Should=" + blkshouldMineMS + ",realblkminesec=" + realblkMineMS + ",eachBlockSec=" + vr.getEachBlockSec + ",TermLeft=" + termblockLeft);
            Thread.sleep(Math.min(maxWaitMS, blkshouldMineMS - realblkMineMS));
          }
          true
        } else {
          if (realblkMineMS > blkshouldMineMS + DConfig.MAX_WAIT_BLK_EPOCH_MS) {
            minerByBlockHeight(block + ((realblkMineMS - blkshouldMineMS) / DConfig.MAX_WAIT_BLK_EPOCH_MS).asInstanceOf[Int]) match {
              case Some(n) =>
                log.debug("Override miner for Next:check:" + blkshouldMineMS + ",realblkmine=" + realblkMineMS + ",n=" + n
                  + ",coaddr=" + coaddr + ",c=" + coaddr + ",blocknext=" + (block + 1) + ",TermLeft=" + termblockLeft);
                coaddr.equals(n)
              case None =>
                log.debug("wait for Miner:Should=" + blkshouldMineMS + ",Real=" + realblkMineMS + ",eachBlockSec=" + vr.getEachBlockSec + ",TermLeft=" + termblockLeft);
                false
            }
          } else {
            log.debug("wait for timeout to Mine:ShouldT=" + (blkshouldMineMS + DConfig.MAX_WAIT_BLK_EPOCH_MS) + ",realblkmine=" + realblkMineMS + ",eachBlockSec=" + vr.getEachBlockSec
              + ",TermLeft=" + termblockLeft);
            if (realblkMineMS < blkshouldMineMS) {
              Thread.sleep(Math.min(maxWaitMS, blkshouldMineMS - realblkMineMS));
            }
            false
          }

        }
      case None =>
        if (maxWaitMS >= 1 && realblkMineMS < blkshouldMineMS) {
          log.debug("wait for time to Mine:Should=" + blkshouldMineMS + ",realblkminesec=" + realblkMineMS + ",eachBlockSec=" + vr.getEachBlockSec + ",TermLeft=" + termblockLeft);
          Thread.sleep(Math.min(maxWaitMS, blkshouldMineMS - realblkMineMS));
        }
        false
    }
  }
  def minerByBlockHeight(block: Int): Option[String] = {
    val vr = voteRequest().getBlockRange;
    if (block >= vr.getStartBlock && block <= vr.getEndBlock) {
      Some(voteRequest().getMinerQueue(block - vr.getStartBlock)
        .getMinerCoaddr)
    } else if (voteRequest().getMinerQueueCount > 0) {
      Some(voteRequest().getMinerQueue(Math.abs(block - vr.getStartBlock)
        % voteRequest().getMinerQueueCount)
        .getMinerCoaddr)
    } else {
      None
    }
  }
  def saveBlock(b: PBlockEntryOrBuilder): Unit = {

    Daos.dposdb.put("D" + b.getBlockHeight, OValue.newBuilder()
      .setCount(b.getBlockHeight)
      .setInfo(b.getSign)
      .setNonce(b.getSliceId)
      .setSecondKey(b.getCoinbaseBcuid)
      .setExtdata(b.getBlockHeader).build())
    log.debug("saveBlockOK:BLK=" + b.getBlockHeight + ",S=" + b.getSliceId + ",CB=" + b.getCoinbaseBcuid
      + ",sign=" + b.getSign)
  }

  def loadFromBlock(block: Int): PBlockEntry.Builder = {
    val ov = Daos.dposdb.get("D" + block).get
    if (ov != null) {
      val b = PBlockEntry.newBuilder().setBlockHeader(ov.getExtdata)
        .setBlockHeight(ov.getCount.asInstanceOf[Int])
        .setSign(ov.getInfo)
        .setSliceId(ov.getNonce)
        .setCoinbaseBcuid(ov.getSecondKey)
      log.debug("load block ok =" + b.getBlockHeight + ",S=" + b.getSliceId + ",CB=" + b.getCoinbaseBcuid
        + ",sign=" + b.getSign)
      b
    } else {
      log.debug("blk not found:" + block);
      null
    }

  }

}