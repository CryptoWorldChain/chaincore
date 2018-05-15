package org.brewchain.account.transaction;

import java.util.ArrayList;
import java.util.List;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tximpl.*;

import com.google.protobuf.ByteString;

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
public class SyncTransactionImpl extends SessionModules<ReqSyncTx> {
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@Override
	public String[] getCmds() {
		return new String[] { PTXTCommand.AYC.name() };
	}

	@Override
	public String getModule() {
		return PTXTModule.TXT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqSyncTx pb, final CompleteHandler handler) {
		RespSyncTx.Builder oRespSyncTx = RespSyncTx.newBuilder();
		oRespSyncTx.setRetCode(1);
		for (MultiTransactionImpl oTransaction : pb.getTxsList()) {
			try {
				MultiTransaction.Builder oMultiTransaction = transactionHelper.parse(oTransaction);
				transactionHelper.SyncTransaction(oMultiTransaction);
			} catch (Exception e) {
				oRespSyncTx.addErrList(oTransaction.getTxHash());
				oRespSyncTx.setRetCode(-1);
			}
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespSyncTx.build()));
	}
}
