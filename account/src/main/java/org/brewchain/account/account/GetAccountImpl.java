package org.brewchain.account.account;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.account.gens.Act.AccountCryptoValue;
import org.brewchain.account.gens.Act.AccountTokenValue;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Actimpl.*;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;

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
public class GetAccountImpl extends SessionModules<ReqGetAccount> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PACTCommand.GAC.name() };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetAccount pb, final CompleteHandler handler) {
		RespGetAccount.Builder oRespGetAccount = RespGetAccount.newBuilder();

		try {
			Account oAccount = oAccountHelper.GetAccount(encApi.hexDec(ByteUtil.formatHexAddress(pb.getAddress())));
			oRespGetAccount.setAddress(encApi.hexEnc(oAccount.getAddress().toByteArray()));
			AccountValue oAccountValue = oAccount.getValue();
			AccountValueImpl.Builder oAccountValueImpl = AccountValueImpl.newBuilder();
			oAccountValueImpl.setAcceptLimit(oAccountValue.getAcceptLimit());
			oAccountValueImpl.setAcceptMax(oAccountValue.getAcceptMax());
			for (ByteString relAddress : oAccountValue.getAddressList()) {
				oAccountValueImpl.addAddress(encApi.hexEnc(relAddress.toByteArray()));
			}

			oAccountValueImpl.setBalance(oAccountValue.getBalance());
			// oAccountValueImpl.setCryptos(index, value)
			for (AccountCryptoValue oAccountTokenValue : oAccountValue.getCryptosList()) {
				AccountCryptoValueImpl.Builder oAccountCryptoValueImpl = AccountCryptoValueImpl.newBuilder();
				oAccountCryptoValueImpl.setSymbol(oAccountTokenValue.getSymbol());

				for (AccountCryptoToken oAccountCryptoToken : oAccountTokenValue.getTokensList()) {
					AccountCryptoTokenImpl.Builder oAccountCryptoTokenImpl = AccountCryptoTokenImpl.newBuilder();
					oAccountCryptoTokenImpl.setCode(oAccountCryptoToken.getCode());
					oAccountCryptoTokenImpl.setHash(encApi.hexEnc(oAccountCryptoToken.getHash().toByteArray()));
					oAccountCryptoTokenImpl.setIndex(oAccountCryptoToken.getIndex());
					oAccountCryptoTokenImpl.setName(oAccountCryptoToken.getName());
					oAccountCryptoTokenImpl.setNonce(oAccountCryptoToken.getNonce());
					oAccountCryptoTokenImpl.setOwner(encApi.hexEnc(oAccountCryptoToken.getOwner().toByteArray()));
					oAccountCryptoTokenImpl.setOwnertime(oAccountCryptoToken.getOwnertime());
					oAccountCryptoTokenImpl.setTimestamp(oAccountCryptoToken.getTimestamp());
					oAccountCryptoTokenImpl.setTotal(oAccountCryptoToken.getTotal());

					oAccountCryptoValueImpl.addTokens(oAccountCryptoTokenImpl);
				}
				oAccountValueImpl.addCryptos(oAccountCryptoValueImpl);
			}
			oAccountValueImpl.setMax(oAccountValue.getMax());
			oAccountValueImpl.setNonce(oAccountValue.getNonce());
			oAccountValueImpl.setPubKey(encApi.hexEnc(oAccountValue.getPubKey().toByteArray()));
			for (AccountTokenValue oAccountTokenValue : oAccountValue.getTokensList()) {
				AccountTokenValueImpl.Builder oAccountTokenValueImpl = AccountTokenValueImpl.newBuilder();
				oAccountTokenValueImpl.setBalance(oAccountTokenValue.getBalance());
				oAccountTokenValueImpl.setToken(oAccountTokenValue.getToken());
				oAccountValueImpl.addTokens(oAccountTokenValueImpl);
			}
			oRespGetAccount.setAccount(oAccountValueImpl);
			oRespGetAccount.setRetCode(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			oRespGetAccount.setRetCode(-1);
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetAccount.build()));
	}
}
