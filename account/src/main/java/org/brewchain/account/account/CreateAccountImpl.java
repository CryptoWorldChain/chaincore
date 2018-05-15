package org.brewchain.account.account;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.gens.Actimpl.PACTCommand;
import org.brewchain.account.gens.Actimpl.PACTModule;
import org.brewchain.account.gens.Actimpl.ReqCreateAccount;
import org.brewchain.account.gens.Actimpl.RespCreateAccount;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;

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
public class CreateAccountImpl extends SessionModules<ReqCreateAccount> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.CAC.name() };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqCreateAccount pb, final CompleteHandler handler) {
		RespCreateAccount.Builder oRespCreateAccount = RespCreateAccount.newBuilder();
		// ExAccountState state = new ExAccountState(BigInteger.ZERO,
		// BigInteger.ZERO);
		try {
			oAccountHelper.CreateAccount(encApi.hexDec(ByteUtil.formatHexAddress(pb.getAddress())), encApi.hexDec(pb.getPubKey()));
			oRespCreateAccount.setRetCode(1);
		} catch (Exception e) {
			e.printStackTrace();
			oRespCreateAccount.setRetCode(-1);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespCreateAccount.build()));
	}
}
