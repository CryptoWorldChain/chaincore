package org.brewchain.account.transaction;

import java.util.List;

import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tximpl.*;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class GetUnSendTransactionImpl extends SessionModules<ReqGetTxToSync> {
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.GUT.name() };
	}

	@Override
	public String getModule() {
		return PTXTModule.TXT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetTxToSync pb, final CompleteHandler handler) {
		RespGetTxToSync.Builder oRespGetTxToSync = RespGetTxToSync.newBuilder();
		oRespGetTxToSync.setTxCount(0);
		try {
			List<MultiTransaction> txs = transactionHelper.getWaitSendTx(pb.getTotal());
			for (int i = 0; i < txs.size(); i++) {
				oRespGetTxToSync.setTxs(i, transactionHelper.parseToImpl(txs.get(i)));
			}
			oRespGetTxToSync.setTxCount(txs.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetTxToSync.build()));
	}
}
