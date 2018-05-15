package org.fc.brewchain.p22p.pbft

import org.fc.brewchain.p22p.utils.LogHelper
import java.util.concurrent.ConcurrentLinkedQueue
import org.fc.brewchain.p22p.pbgens.P22P.PVBase
import org.brewchain.bcapi.gens.Oentity.OValue
import org.fc.brewchain.p22p.pbgens.P22P.PBFTStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.fc.brewchain.p22p.node.Network
import org.fc.brewchain.p22p.node.Networks
import java.util.concurrent.ConcurrentHashMap

case class VoteQueue(network: Network) extends LogHelper {

  val inQ = new LinkedBlockingQueue[(PVBase, OValue.Builder, PBFTStage)](); //,new LinkedBlockingQueue[(Network,PVBase, OValue.Builder, PBFTStage)]]();
  val outQ = new ConcurrentLinkedQueue[PVBase]();

  def appendInQ(pbo: PVBase) = {
    val network = Networks.networkByID(pbo.getNid);

    network.stateStorage.mergeViewState(pbo) match {
      case Some(ov) if ov == null =>
        log.debug("drop message because ov is null:V=" + pbo.getV + ",S=" + pbo.getState + ",F=" + pbo.getFromBcuid + ",O=" + pbo.getOriginBcuid
          + ",RJ=" + pbo.getRejectState)
        PBFTStage.NOOP
      case Some(ov) if ov != null =>
        pbo.getState match {
          case PBFTStage.PENDING_SEND =>
            inQ.offer((pbo, ov, PBFTStage.PRE_PREPARE));

          case PBFTStage.PRE_PREPARE =>
            if (network.stateStorage.updateNodeStage(pbo, PBFTStage.PRE_PREPARE) != PBFTStage.DUPLICATE) {
              if (pbo.getRejectState != PBFTStage.REJECT) {
                inQ.offer((pbo, ov, PBFTStage.PREPARE));
                log.debug("Qsize=" + inQ.size())
              } else {
                inQ.offer((pbo, ov, PBFTStage.REJECT));
              }
            }

          case PBFTStage.PREPARE =>
            if (network.stateStorage.updateNodeStage(pbo, pbo.getState) != PBFTStage.DUPLICATE) {
              if (pbo.getRejectState != PBFTStage.REJECT) {
                inQ.offer((pbo, ov, PBFTStage.COMMIT));
              } else {
                inQ.offer((pbo, ov, PBFTStage.REJECT));
              }
            }
          case PBFTStage.COMMIT =>
            network.stateStorage.updateNodeStage(pbo, pbo.getState)
            //            if (pbo.getRejectState != PBFTStage.REJECT) {
            inQ.offer((pbo, ov, PBFTStage.REPLY));
          //            }
          case PBFTStage.REPLY =>
            network.stateStorage.saveStageV(pbo, ov.build());
            log.info("MergeSuccess.Remote!:V=" + pbo.getV + ",N=" + pbo.getN + ",org=" + pbo.getOriginBcuid)
            PBFTStage.NOOP
          case _ =>
            PBFTStage.NOOP
        }

      case None =>
        if (pbo.getRejectState != PBFTStage.REJECT) {
          inQ.offer((pbo, null, PBFTStage.REJECT));
        }
        PBFTStage.REJECT
    }

  }

  def pollQ(): (PVBase, OValue.Builder, PBFTStage) = {
    inQ.poll(20, TimeUnit.SECONDS)
  }

  def appendOutQ(pbo: PVBase) = {
    outQ.offer(pbo);
  }

}