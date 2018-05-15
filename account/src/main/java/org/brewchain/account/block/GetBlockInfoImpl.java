package org.brewchain.account.block;

import org.brewchain.account.core.BlockChainHelper;
import org.brewchain.account.core.WaitBlockHashMapDB;
import org.brewchain.account.core.WaitSendHashMapDB;
import org.brewchain.account.gens.Block.PBCTCommand;
import org.brewchain.account.gens.Block.PBCTModule;
import org.brewchain.account.gens.Block.ReqBlockInfo;
import org.brewchain.account.gens.Block.RespBlockInfo;
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
public class GetBlockInfoImpl extends SessionModules<ReqBlockInfo> {

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	BlockChainHelper blockChainHelper;

	@ActorRequire(name = "WaitSend_HashMapDB", scope = "global")
	WaitSendHashMapDB oSendingHashMapDB; // 保存待广播交易
	@ActorRequire(name = "WaitBlock_HashMapDB", scope = "global")
	WaitBlockHashMapDB oPendingHashMapDB; // 保存待打包block的交易

	@Override
	public String[] getCmds() {
		return new String[] { PBCTCommand.BIO.name() };
	}

	@Override
	public String getModule() {
		return PBCTModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqBlockInfo pb, final CompleteHandler handler) {
		RespBlockInfo.Builder oRespBlockInfo = RespBlockInfo.newBuilder();
		oRespBlockInfo.setBlockCount(blockChainHelper.getBlockCount());
		try {
			oRespBlockInfo.setNumber(blockChainHelper.getLastBlockNumber());
			oRespBlockInfo.setCache(blockChainHelper.getBlockCacheFormatString());
			oRespBlockInfo.setWaitSync(oSendingHashMapDB.keys().size());
			oRespBlockInfo.setWaitBlock(oPendingHashMapDB.keys().size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespBlockInfo.build()));
	}
}
