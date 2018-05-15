  package org.fc.brewchain.p22p.pbft

import java.util.concurrent.atomic.AtomicInteger
import org.fc.brewchain.p22p.Daos
import org.apache.commons.lang3.StringUtils
import onight.oapi.scala.traits.OLog
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import org.fc.brewchain.p22p.pbgens.P22P.PBFTStage
import scala.concurrent.impl.Future
import com.google.protobuf.ByteString
import onight.tfw.otransio.api.PacketHelper
import org.fc.brewchain.p22p.pbgens.P22P.PVType
import org.fc.brewchain.p22p.pbgens.P22P.PVBaseOrBuilder
import org.fc.brewchain.bcapi.JodaTimeHelper
import org.brewchain.bcapi.gens.Oentity.OValue
import org.brewchain.bcapi.gens.Oentity.OPair
import java.util.ArrayList
import scala.language.implicitConversions
import scala.collection.JavaConversions._
import org.fc.brewchain.p22p.core.Votes
import org.fc.brewchain.p22p.core.Votes.VoteResult
import org.fc.brewchain.p22p.core.Votes.NotConverge
import org.brewchain.bcapi.gens.Oentity.OValueOrBuilder
import org.fc.brewchain.p22p.core.Votes.Converge
import org.fc.brewchain.p22p.core.Votes.Undecisible
import onight.tfw.mservice.NodeHelper
import org.fc.brewchain.p22p.utils.Config
import org.apache.commons.codec.binary.Base64
import org.fc.brewchain.p22p.node.Network

case class StateStorage(network: Network) extends OLog {
  def STR_seq(pbo: PVBaseOrBuilder): String = STR_seq(pbo.getMTypeValue)
  def STR_seq(uid: Int): String = "v_seq_" + network.netid + "." + uid

  def nextV(pbo: PVBase.Builder): Int = {
    this.synchronized {
      val (retv, newstate) = Daos.viewstateDB.get(STR_seq(pbo)).get match {
        case ov if ov != null =>
          PVBase.newBuilder().mergeFrom(ov.getExtdata) match {
            case dbpbo if dbpbo.getState == PBFTStage.REPLY =>
              if (System.currentTimeMillis() - dbpbo.getLastUpdateTime < Config.MIN_EPOCH_EACH_VOTE) {
                log.debug("cannot start next vote: time less than past" + dbpbo.getState + ",V=" + dbpbo.getV + ",DIS=" + JodaTimeHelper.secondFromNow(dbpbo.getLastUpdateTime));
                (-1, PBFTStage.REJECT);
              } else {
                log.debug("getting next vote:" + dbpbo.getState + ",V=" + dbpbo.getV);
                pbo.setViewCounter(dbpbo.getViewCounter + 1)
                pbo.setStoreNum(dbpbo.getStoreNum)
                (dbpbo.getV + 1, PBFTStage.PRE_PREPARE)
              }
            case dbpbo if System.currentTimeMillis() - dbpbo.getLastUpdateTime < Config.MIN_EPOCH_EACH_VOTE && dbpbo.getState == PBFTStage.INIT || System.currentTimeMillis() - dbpbo.getCreateTime > Config.TIMEOUT_STATE_VIEW =>
              log.debug("recover from vote:" + dbpbo.getState + ",lastCreateTime:" + JodaTimeHelper.format(dbpbo.getCreateTime));
              pbo.setViewCounter(dbpbo.getViewCounter)
              pbo.setStoreNum(dbpbo.getStoreNum)
              getStageV(pbo.build()) match {
                case fv if fv == null =>
                  (dbpbo.getV, PBFTStage.PRE_PREPARE)
                case fv if fv != null =>
                  (dbpbo.getV + 1, PBFTStage.PRE_PREPARE)
              }
            case dbpbo =>
              log.debug("cannot start vote:" + dbpbo.getState + ",past=" + JodaTimeHelper.secondFromNow(dbpbo.getCreateTime) + ",O=" + dbpbo.getOriginBcuid);
              (-1, PBFTStage.REJECT);
          }
        case _ =>
          log.debug("New State ,db is empty");
          pbo.setViewCounter(1)
          pbo.setStoreNum(1)
          (1, PBFTStage.PRE_PREPARE);
      }
      if (Config.VOTE_DEBUG) return -1;
      if (retv > 0) {
        Daos.viewstateDB.put(STR_seq(pbo),
          OValue.newBuilder().setCount(retv) //
            .setExtdata(ByteString.copyFrom(pbo.setV(retv)
              .setCreateTime(System.currentTimeMillis())
              .setLastUpdateTime(System.currentTimeMillis())
              .setFromBcuid(network.root().bcuid)
              .setOriginBcuid(network.root().bcuid)
              .setState(newstate)
              .build().toByteArray()))
            .build());
      }
      retv
    }
  }

  def mergeViewState(pbo: PVBase): Option[OValue.Builder] = {
    Daos.viewstateDB.get(STR_seq(pbo)).get match {
      case ov if ov != null && StringUtils.equals(pbo.getOriginBcuid, network.root().bcuid) =>
        Some(ov.toBuilder()) // from locals
      case ov if ov != null =>
        PVBase.newBuilder().mergeFrom(ov.getExtdata) match {
          case dbpbo if StringUtils.equals(dbpbo.getOriginBcuid, pbo.getOriginBcuid) && pbo.getV == dbpbo.getV
            && dbpbo.getStateValue <= pbo.getStateValue =>
            Some(ov.toBuilder());
          case dbpbo if StringUtils.equals(dbpbo.getOriginBcuid, pbo.getOriginBcuid) && pbo.getV == dbpbo.getV
            && dbpbo.getStateValue > pbo.getStateValue =>
            log.debug("state low dbV=" + dbpbo.getStateValue + ",pbV=" + pbo.getStateValue + ",V=" + pbo.getV + ",f=" + pbo.getFromBcuid)
            Some(ov.toBuilder())
          case dbpbo if (System.currentTimeMillis() - dbpbo.getLastUpdateTime > Config.TIMEOUT_STATE_VIEW)
            && pbo.getV >= dbpbo.getV =>
            Some(ov.toBuilder());
          case dbpbo if pbo.getV >= dbpbo.getV && (
            dbpbo.getState == PBFTStage.INIT || dbpbo.getState == PBFTStage.REPLY
            || StringUtils.isBlank(dbpbo.getOriginBcuid)) =>
            Some(ov.toBuilder());
          case dbpbo if (dbpbo.getState == PBFTStage.REJECT || dbpbo.getRejectState == PBFTStage.REJECT)
            && !StringUtils.equals(dbpbo.getOriginBcuid, pbo.getOriginBcuid) =>
            Some(ov.toBuilder());
          case dbpbo @ _ =>
            pbo.getState match {
              case PBFTStage.COMMIT => //already know by other.
                log.debug("other nodes commited!")
                updateNodeStage(pbo, PBFTStage.COMMIT);
                voteNodeStages(pbo) match {
                  case n: Converge if n.decision == pbo.getState =>
                    log.debug("OtherVoteCommit::MergeOK,PS=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",org_bcuid=" + pbo.getOriginBcuid + ",from=" + pbo.getFromBcuid);
                    Daos.viewstateDB.put(STR_seq(pbo),
                      OValue.newBuilder().setCount(pbo.getN) //
                        .setExtdata(ByteString.copyFrom(pbo.toBuilder()
                          .setFromBcuid(network.root().bcuid)
                          .setState(PBFTStage.INIT)
                          .setV(pbo.getV).setStoreNum(pbo.getStoreNum).setViewCounter(pbo.getViewCounter)
                          .build().toByteArray()))
                        .build())
                    None;
                  case _ =>
                    log.debug("OtherVoteCommit::NotMerge,PS=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",org_bcuid=" + pbo.getOriginBcuid + ",from=" + pbo.getFromBcuid);
                    None;
                }
              case _ =>
                log.debug("Cannot MergeView For local state Not EQUAL:"
                  + "db.[O=" + dbpbo.getOriginBcuid + ",F=" + dbpbo.getFromBcuid + ",V=" + dbpbo.getV + ",S=" + dbpbo.getStateValue
                  + ",RJ=" + dbpbo.getRejectState
                  + ",TO=" + JodaTimeHelper.secondFromNow(dbpbo.getLastUpdateTime)
                  + "],p[O=" + pbo.getOriginBcuid + ",F=" + pbo.getFromBcuid + ",V=" + pbo.getV + ",S=" + pbo.getStateValue
                  + ",RJ=" + pbo.getRejectState + "]");
                None;
            }

        }
      case _ =>
        val ov = OValue.newBuilder().setCount(pbo.getN) //
          .setExtdata(ByteString.copyFrom(pbo.toByteArray()))
        Some(ov)
    }
  }

  def saveStageV(pbo: PVBase, ov: OValue) {
    val key = STR_seq(pbo) + ".F." + pbo.getV;
    //    val dbov = Daos.viewstateDB.get(key).get
    //    log.debug("saveStage:V=" + pbo.getV + ",O=" + pbo.getOriginBcuid)
    //    if (dbov != null) {
    //      val pb = PVBase.newBuilder().mergeFrom(ov.getExtdata);
    //      pb.setContents(ByteString.copyFrom(Base64.encodeBase64(pb.getContents.toByteArray()))).setLastUpdateTime(System.currentTimeMillis())
    //      log.warn("Already Save Stage!" + dbov + ",pb=" + pb)
    //    }
    Daos.viewstateDB.put(STR_seq(pbo) + ".F." + pbo.getV, ov);
  }

  def getStageV(pbo: PVBase) {
    val key = STR_seq(pbo) + ".F." + pbo.getV;
    //    val dbov = Daos.viewstateDB.get(key).get
    //    log.debug("saveStage:V=" + pbo.getV + ",O=" + pbo.getOriginBcuid)
    //    if (dbov != null) {
    //      val pb = PVBase.newBuilder().mergeFrom(ov.getExtdata);
    //      pb.setContents(ByteString.copyFrom(Base64.encodeBase64(pb.getContents.toByteArray()))).setLastUpdateTime(System.currentTimeMillis())
    //      log.warn("Already Save Stage!" + dbov + ",pb=" + pb)
    //    }
    Daos.viewstateDB.get(STR_seq(pbo) + ".F." + pbo.getV).get;
  }
  //  def updateLocalViewState(pbo: PVBase, ov: OValue.Builder, newstate: PBFTStage)(implicit dm: Votable = null): PBFTStage = {
  //    updateNodeStage(pbo, pbo.getState)
  //    makeVote(pbo, ov, newstate)(dm)
  //  }
  def updateTopViewState(pbo: PVBase) {
    this.synchronized({
      val ov = Daos.viewstateDB.get(STR_seq(pbo)).get
      if (ov != null) {
        Daos.viewstateDB.put(STR_seq(pbo),
          ov.toBuilder().clone().clearSecondKey()
            .setExtdata(pbo.toBuilder().setLastUpdateTime(System.currentTimeMillis()).build().toByteString()).build());
      }
    })
  }

  def saveIfNotExist(pbo: PVBase, ov: OValue.Builder, newstate: PBFTStage): PBFTStage = {
    val dbkey = STR_seq(pbo) + "." + pbo.getOriginBcuid + "." + pbo.getMessageUid + "." + pbo.getV + "." + newstate;
    Daos.viewstateDB.get(dbkey).get match {
      case ov if ov != null => //&& PVBase.newBuilder().mergeFrom(ov.getExtdata).getState == newstate =>
        log.debug("Omit duplicated=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);
        PBFTStage.DUPLICATE;
      case _ =>
        Daos.viewstateDB.put(dbkey, ov.build());
        newstate
    }
  }
  def makeVote(pbo: PVBase, ov: OValue.Builder, newstate: PBFTStage)(implicit dm: Votable = null): PBFTStage = {
    //    val dmresult = if (dm != null && pbo.getState == PBFTStage.PREPARE) dm.makeDecision(pbo) else 0
    //    val dbkey = STR_seq(pbo) + "." + pbo.getFromBcuid + "." + pbo.getMessageUid + "." + pbo.getV + ".";
    val dbkey = STR_seq(pbo) + "." + pbo.getOriginBcuid + "." + pbo.getMessageUid + "." + pbo.getV + ".";
    Daos.viewstateDB.get(dbkey + newstate).get match {
      case ov if ov != null => //&& PVBase.newBuilder().mergeFrom(ov.getExtdata).getState == newstate =>
        //        log.debug("Omit duplicated=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);
        PBFTStage.DUPLICATE;
      case _ =>
        //        Daos.viewstateDB.get(dbkey + pbo.getState).get match {
        //          case ov if ov != null =>
        //            log.debug("Omit duplicated=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);
        //
        //            PBFTStage.DUPLICATE;
        //          case _ =>
        voteNodeStages(pbo) match {
          case n: Converge if n.decision == pbo.getState =>
            ov.setExtdata(
              ByteString.copyFrom(pbo.toBuilder()
                .setState(newstate)
                .setLastUpdateTime(System.currentTimeMillis())
                .build().toByteArray()))
            log.debug("Vote::MergeOK,PS=" + pbo.getState + ",New=" + newstate + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid + ",from=" + pbo.getFromBcuid);
            Daos.viewstateDB.put(STR_seq(pbo), ov.clone().clearSecondKey().build());
            if (newstate != PBFTStage.PRE_PREPARE && newstate != PBFTStage.PREPARE) {
              saveIfNotExist(pbo, ov, newstate);
            }
            newstate

          case un: Undecisible =>
            log.debug("Vote::Undecisible:State=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);
            PBFTStage.NOOP
          case no: NotConverge =>
            log.debug("Vote::Not Converge:State=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);

            ov.setExtdata(
              ByteString.copyFrom(pbo.toBuilder()
                .setRejectState(PBFTStage.NOOP)
                .setLastUpdateTime(System.currentTimeMillis())
                .build().toByteArray()))
            Daos.viewstateDB.put(dbkey + pbo.getState, ov.build());

            if (StringUtils.equals(pbo.getOriginBcuid, network.root().bcuid)) {
              log.debug("Reject for My Vote ")
              Daos.viewstateDB.put(STR_seq(pbo),
                OValue.newBuilder().setCount(pbo.getN) //
                  .setExtdata(ByteString.copyFrom(pbo.toBuilder()
                    .setFromBcuid(network.root().bcuid)
                    .setState(PBFTStage.INIT)
                    .setLastUpdateTime(System.currentTimeMillis() + Config.getRandSleepForBan())
                    .build().toByteArray()))
                  .build())
            }
            PBFTStage.NOOP
          case n: Converge if n.decision == PBFTStage.REJECT =>
            ov.setExtdata(
              ByteString.copyFrom(pbo.toBuilder()
                .setRejectState(PBFTStage.REJECT)
                .setLastUpdateTime(System.currentTimeMillis())
                .build().toByteArray()))
            Daos.viewstateDB.put(dbkey + pbo.getState, ov.build());
            log.warn("getRject ConvergeState:" + n.decision + ",NewState=" + newstate + ",pbostate=" + pbo.getState + ",V=" + pbo.getV + ",N=" + pbo.getN + ",SN=" + pbo.getStoreNum + ",VC=" + pbo.getViewCounter + ",org_bcuid=" + pbo.getOriginBcuid);
            if (StringUtils.equals(pbo.getOriginBcuid, network.root().bcuid)) {
              log.debug("Reject for this Vote ")
              Daos.viewstateDB.put(STR_seq(pbo),
                OValue.newBuilder().setCount(pbo.getN) //
                  .setExtdata(ByteString.copyFrom(pbo.toBuilder()
                    .setFromBcuid(network.root().bcuid)
                    .setState(PBFTStage.INIT)
                    .setLastUpdateTime(System.currentTimeMillis() + Config.getRandSleepForBan())
                    .build().toByteArray()))
                  .build())
            } else {
              log.debug("Reject for other Vote ")
              Daos.viewstateDB.put(STR_seq(pbo),
                OValue.newBuilder().setCount(pbo.getN) //
                  .setExtdata(ByteString.copyFrom(pbo.toBuilder()
                    .setFromBcuid(network.root().bcuid)
                    .setState(PBFTStage.REJECT).setRejectState(PBFTStage.REJECT)
                    .setLastUpdateTime(System.currentTimeMillis() + Config.getRandSleepForBan())
                    .build().toByteArray()))
                  .build())

            }
            PBFTStage.REJECT
          case n: Converge =>
            log.warn("unknow ConvergeState:" + n.decision + ",NewState=" + newstate + ",pbostate=" + pbo.getState);
            PBFTStage.NOOP
          case _ =>
            PBFTStage.NOOP
        }
      //        }
    }
  }
  def updateNodeStage(pbo: PVBase, state: PBFTStage): PBFTStage = {
    val strkey = STR_seq(pbo);
    val newpbo = if (state != pbo.getState) pbo.toBuilder().setState(state).setLastUpdateTime(System.currentTimeMillis()).build()
    else
      pbo;
    val ov = OValue.newBuilder().setCount(pbo.getN) //
    ov.setExtdata(ByteString.copyFrom(newpbo.toByteArray()))
      .setSecondKey(strkey + "." + pbo.getOriginBcuid + "." + pbo.getMessageUid + "." + pbo.getV)
      .setDecimals(newpbo.getStateValue);
    Daos.viewstateDB.put(strkey + "." + pbo.getFromBcuid + "." + pbo.getMessageUid + "." + pbo.getV + "." + newpbo.getStateValue, ov.build());
    state
  }
  def outputList(ovs: List[OPair]): Unit = {
    ovs.map { x =>
      val p = PVBase.newBuilder().mergeFrom(x.getValue.getExtdata);
      log.debug("-------::DBList:State=" + p.getState + ",V=" + p.getV + ",N=" + p.getN + ",SN=" + p.getStoreNum + ",VC=" + p.getViewCounter + ",O=" + p.getOriginBcuid
        + ",F=" + p.getFromBcuid + ",REJRECT=" + p.getRejectState + ",KEY=" + new String(x.getKey.getData.toByteArray()))
    }
  }
  def voteNodeStages(pbo: PVBase)(implicit dm: Votable = null): VoteResult = {
    val strkey = STR_seq(pbo);
    val ovs = Daos.viewstateDB.listBySecondKey(strkey + "." + pbo.getOriginBcuid + "." + pbo.getMessageUid + "." + pbo.getV);
    if (ovs.get != null && ovs.get.size() > 0) {
      val reallist = ovs.get.filter { ov => ov.getValue.getDecimals == pbo.getStateValue }.toList;
      log.debug("get list:allsize=" + ovs.get.size() + ",statesize=" + reallist.size + ",state=" + pbo.getState)
      //      outputList(ovs.get.toList)
      if (dm != null) { //Vote only pass half
        dm.voteList(network, pbo, reallist)
      } else {
        Undecisible()
      }
    } else {
      Undecisible()
    }
  }
  //
  //  def vote(pbo: PVBase): PBFTStage = {
  //    this.synchronized {
  //      mergeViewState(pbo) match {
  //        case Some(ov) if ov == null =>
  //          PBFTStage.NOOP
  //        case Some(ov) if ov != null =>
  //          pbo.getState match {
  //            case PBFTStage.PRE_PREPARE =>
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              //updateLocalViewState(pbo, ov, PBFTStage.PREPARE)
  //              updateNodeStage(pbo, PBFTStage.PREPARE)
  //              if (pbo.getRejectState == PBFTStage.REJECT) {
  //                PBFTStage.NOOP
  //              } else {
  //                PBFTStage.PREPARE
  //              }
  //            case PBFTStage.PREPARE =>
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              updateLocalViewState(pbo, ov, PBFTStage.COMMIT)
  //            case PBFTStage.COMMIT =>
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              updateLocalViewState(pbo, ov, PBFTStage.REPLY)
  //            case PBFTStage.REPLY =>
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //              saveStageV(pbo, ov.build());
  //              PBFTStage.NOOP
  //            //            case PBFTStage.REJECT =>
  //            //              log.debug("get Reject from=" + pbo.getFromBcuid + ",V=" + pbo.getV + ",O=" + pbo.getOriginBcuid + ",OLDState=" + pbo.getOldState);
  //            //            Daos.viewstateDB.put(STR_seq(pbo), ov.build());
  //            //              updateLocalViewState(pbo, ov, pbo.getOldState)
  //            //              PBFTStage.NOOP
  //            case _ =>
  //              PBFTStage.NOOP
  //          }
  //
  //        case None =>
  //          PBFTStage.REJECT
  //      }
  //    }
  //
  //  }
  val VIEW_ID_PROP = "org.bc.pbft.view.state"

}