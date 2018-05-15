package org.brewchain.account.block;

import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Block.PBCTCommand;
import org.brewchain.account.gens.Block.PBCTModule;
import org.brewchain.account.gens.Block.ReqGetBlock;
import org.brewchain.account.gens.Block.RespGetBlock;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

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
public class GetBlockImpl extends SessionModules<ReqGetBlock> {
	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper blockHelper;
	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PBCTCommand.GBC.name() };
	}

	@Override
	public String getModule() {
		return PBCTModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqGetBlock pb, final CompleteHandler handler) {
		RespGetBlock.Builder oRespGetBlock = RespGetBlock.newBuilder();

		try {
			String coinBase = this.props().get("block.coinBase.hex", null);
			if (coinBase == null) {
				oRespGetBlock.setRetCode(-2);
				handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
				return;
			}
			BlockEntity.Builder oBlockEntity;
			try {
				oBlockEntity = blockHelper.CreateNewBlock(pb.getTxCount(), encApi.hexDec(pb.getExtraData()),
						ByteString.copyFromUtf8(coinBase).toByteArray());
				oRespGetBlock.setHeader(oBlockEntity.getHeader());
				oRespGetBlock.setRetCode(1);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (Exception e) {
			oRespGetBlock.setRetCode(-1);
			e.printStackTrace();
		}

		oRespGetBlock.setRetCode(1);

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespGetBlock.build()));
	}
}
