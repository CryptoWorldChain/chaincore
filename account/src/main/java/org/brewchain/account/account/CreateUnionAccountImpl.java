package org.brewchain.account.account;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import org.brewchain.account.gens.Actimpl.PACTCommand;
import org.brewchain.account.gens.Actimpl.PACTModule;
import org.brewchain.account.gens.Actimpl.ReqCreateUnionAccount;
import org.brewchain.account.gens.Actimpl.RespCreateUnionAccount;
import org.brewchain.account.gens.Tx.SingleTransaction;

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
public class CreateUnionAccountImpl extends SessionModules<ReqCreateUnionAccount> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.UAC.name() };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateUnionAccount pb, final CompleteHandler handler) {
		RespCreateUnionAccount.Builder oRespCreateUnionAccount = RespCreateUnionAccount.newBuilder();
		try {
			
			// 创建多重签名账户
			List<ByteString> relAddresses = new ArrayList<ByteString>();
			for (String addressString : pb.getRelAddressList()) {
				relAddresses.add(ByteString.copyFrom(encApi.hexDec(ByteUtil.formatHexAddress(addressString))));
			}

			accountHelper.CreateUnionAccount(encApi.hexDec(pb.getAddress()), ByteUtil.EMPTY_BYTE_ARRAY, pb.getMax(),
					pb.getAcceptMax(), pb.getAcceptLimit(), relAddresses);

			oRespCreateUnionAccount.setRetCode(1);
		} catch (Exception e) {
			e.printStackTrace();
			oRespCreateUnionAccount.setRetCode(-1);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateUnionAccount.build()));
	}
}
