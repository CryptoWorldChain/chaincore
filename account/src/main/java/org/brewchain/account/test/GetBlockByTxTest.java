package org.brewchain.account.test;

import java.util.Date;
import java.util.List;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockChainHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.gens.TxTest.PTSTCommand;
import org.brewchain.account.gens.TxTest.PTSTModule;
import org.brewchain.account.gens.TxTest.ReqTxTest;
import org.brewchain.account.gens.TxTest.RespTxTest;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class GetBlockByTxTest extends SessionModules<ReqTxTest> implements ActorService {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;

	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper blockHelper;

	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	BlockChainHelper blockChainHelper;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.GBX.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqTxTest pb, final CompleteHandler handler) {
		RespTxTest.Builder oRespTxTest = RespTxTest.newBuilder();
		KeyPairs oKeyPairs1 = encApi.genKeys();
		KeyPairs oKeyPairs2 = encApi.genKeys();
		ByteString txHash;
		// try {
		// accountHelper.CreateAccount(encApi.hexDec(oKeyPairs1.getAddress()),
		// encApi.hexDec(oKeyPairs1.getPubkey()));
		// accountHelper.CreateAccount(encApi.hexDec(oKeyPairs2.getAddress()),
		// encApi.hexDec(oKeyPairs2.getPubkey()));
		//
		// accountHelper.addBalance(encApi.hexDec(oKeyPairs1.getAddress()),
		// 100000);
		// accountHelper.addBalance(encApi.hexDec(oKeyPairs2.getAddress()),
		// 100000);
		// // 创建几个交易
		// int nonce =
		// accountHelper.getNonce(encApi.hexDec(oKeyPairs1.getAddress()));
		//
		// MultiTransaction.Builder oMultiTransaction =
		// MultiTransaction.newBuilder();
		// MultiTransactionBody.Builder oMultiTransactionBody =
		// MultiTransactionBody.newBuilder();
		// MultiTransactionInput.Builder oMultiTransactionInput4 =
		// MultiTransactionInput.newBuilder();
		// oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairs1.getAddress())));
		// oMultiTransactionInput4.setAmount(100);
		// oMultiTransactionInput4.setFee(0);
		// oMultiTransactionInput4.setFeeLimit(0);
		// oMultiTransactionInput4.setNonce(nonce);
		// oMultiTransactionBody.addInputs(oMultiTransactionInput4);
		//
		// MultiTransactionOutput.Builder oMultiTransactionOutput1 =
		// MultiTransactionOutput.newBuilder();
		// oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairs2.getAddress())));
		// oMultiTransactionOutput1.setAmount(100);
		// oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);
		//
		// oMultiTransactionBody.setData(ByteString.EMPTY);
		// oMultiTransaction.setTxHash(ByteString.EMPTY);
		// oMultiTransactionBody.clearSignatures();
		//
		// oMultiTransactionBody.setTimestamp((new Date()).getTime());
		// // 签名
		// MultiTransactionSignature.Builder oMultiTransactionSignature21 =
		// MultiTransactionSignature.newBuilder();
		// oMultiTransactionSignature21.setPubKey(oKeyPairs1.getPubkey());
		// oMultiTransactionSignature21.setSignature(
		// encApi.hexEnc(encApi.ecSign(oKeyPairs1.getPrikey(),
		// oMultiTransactionBody.build().toByteArray())));
		// oMultiTransactionBody.addSignatures(oMultiTransactionSignature21);
		//
		// oMultiTransaction.setTxBody(oMultiTransactionBody);
		// txHash = transactionHelper.CreateMultiTransaction(oMultiTransaction);
		// } catch (Exception e) {
		// // TODO: handle exception
		// return;
		// }
		//
		// BlockEntity.Builder oSyncBlock = BlockEntity.newBuilder();
		// BlockEntity.Builder newBlock;
		// try {
		// newBlock = blockHelper.CreateNewBlock(600, ByteUtil.EMPTY_BYTE_ARRAY,
		// ByteString.copyFromUtf8("12345").toByteArray());
		// oSyncBlock.setHeader(newBlock.getHeader());
		// oSyncBlock.setBody(newBlock.getBody());
		// log.debug(String.format("==> 第 %s 块 hash %s 创建成功",
		// oSyncBlock.getHeader().getNumber(),
		// encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray())));
		// blockHelper.ApplyBlock(oSyncBlock.build());
		// log.debug(String.format("==> 第 %s 块 hash %s 父hash %s 交易 %s 笔",
		// oSyncBlock.getHeader().getNumber(),
		// encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray()),
		// encApi.hexEnc(oSyncBlock.getHeader().getParentHash().toByteArray()),
		// oSyncBlock.getHeader().getTxHashsCount()));
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		//
		// try {
		// BlockEntity oBlockEntity =
		// blockHelper.getBlockByTransaction(txHash.toByteArray());
		// log.debug(String.format("===> block ",
		// oBlockEntity.getHeader().getBlockHash().toStringUtf8()));
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		try {
//			BlockEntity oBlockEntity1 = blockChainHelper.getBlockByNumber(1);
//			BlockEntity oBlockEntity4 = blockChainHelper.getBlockByNumber(4);
//
//			List<BlockEntity> list1 = blockChainHelper.getBlocks(oBlockEntity1.getHeader().getBlockHash().toByteArray(),
//					oBlockEntity4.getHeader().getBlockHash().toByteArray(), 100);
//			log.debug("list count " + list1.size());
			
			BlockEntity oBlockEntity1 = blockChainHelper.getBlockByNumber(0);
//			BlockEntity oBlockEntity26 = blockChainHelper.getBlockByNumber(26);
//
//			List<BlockEntity> list1 = blockChainHelper.getParentsBlocks(oBlockEntity26.getHeader().getBlockHash().toByteArray(),
//					oBlockEntity1.getHeader().getBlockHash().toByteArray(), 100);
			log.debug("list count ");
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRespTxTest.build()));
	}
}
