package org.brewchain.account.sys;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.account.gens.Sys.PSYSCommand;
import org.brewchain.account.gens.Sys.PSYSModule;
import org.brewchain.account.gens.Sys.ReqAddCryptoToken;
import org.brewchain.account.gens.Sys.RespAddCryptoToken;
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
public class AddCryptoTokenImpl extends SessionModules<ReqAddCryptoToken> {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper oAccountHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PSYSCommand.ACB.name() };
	}

	@Override
	public String getModule() {
		return PSYSModule.SYS.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqAddCryptoToken pb, final CompleteHandler handler) {
		RespAddCryptoToken.Builder oRespAddCryptoToken = RespAddCryptoToken.newBuilder();

		for (int i = 0; i < pb.getCodeCount(); i++) {
			AccountCryptoToken.Builder oAccountCryptoToken = AccountCryptoToken.newBuilder();
			oAccountCryptoToken.setCode(pb.getCode(i));
			oAccountCryptoToken.setIndex(pb.getIndex(i));
			oAccountCryptoToken.setName(pb.getName(i));
			oAccountCryptoToken.setTimestamp(pb.getTimestamp(i));
			oAccountCryptoToken.setTotal(pb.getTotal(i));
			encApi.sha256Encode(oAccountCryptoToken.build().toByteArray());

			oAccountCryptoToken.setOwner(ByteString.copyFrom(encApi.hexDec(pb.getHexAddress())));
			oAccountCryptoToken.setNonce(0);

			try {
				oAccountHelper.addCryptoBalance(oAccountCryptoToken.build().getOwner().toByteArray(), pb.getSymbol(i),
						oAccountCryptoToken.build().toByteArray());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				oRespAddCryptoToken.setTotal(i + 1);
				oRespAddCryptoToken.setRetCode(-1);
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespAddCryptoToken.build()));

				return;
			}
		}
		
		oRespAddCryptoToken.setRetCode(1);
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespAddCryptoToken.build()));
		// oAccountCryptoToken.setCode(pb.getc)
	}
}
