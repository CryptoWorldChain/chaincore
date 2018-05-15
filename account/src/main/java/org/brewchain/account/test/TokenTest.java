package org.brewchain.account.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.gens.Tx.SingleTransaction;
import org.brewchain.account.gens.TxTest.PTSTCommand;
import org.brewchain.account.gens.TxTest.PTSTModule;
import org.brewchain.account.gens.TxTest.ReqTxTest;
import org.brewchain.account.gens.TxTest.RespTxTest;
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
public class TokenTest extends SessionModules<ReqTxTest> implements ActorService {
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;

	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;

	@ActorRequire(name = "Block_Helper", scope = "global")
	BlockHelper blockHelper;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	@Override
	public String[] getCmds() {
		return new String[] { PTSTCommand.TOT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqTxTest pb, final CompleteHandler handler) {
		RespTxTest.Builder oRespTxTest = RespTxTest.newBuilder();
		String coinBase = this.props().get("block.coinBase", "");
		if (coinBase == null) {
			coinBase = "1234";
		}

		try {
			blockHelper.CreateGenesisBlock(new LinkedList<MultiTransaction>(), ByteUtil.EMPTY_BYTE_ARRAY);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// 创建账户1
		KeyPairs oKeyPairs1 = encApi.genKeys();
		// 创建账户1
		accountHelper.CreateAccount(oKeyPairs1.getAddress().getBytes(), oKeyPairs1.getPubkey().getBytes());
		// 增加账户余额1
		try {
			accountHelper.addTokenBalance(oKeyPairs1.getAddress().getBytes(), "ABC", 10000000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 创建账户2
		KeyPairs oKeyPairs2 = encApi.genKeys();
		// 创建账户2
		accountHelper.CreateAccount(oKeyPairs2.getAddress().getBytes(), oKeyPairs2.getPubkey().getBytes());
		// 增加账户余额2
		try {
			accountHelper.addTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF", 10000000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MultiTransaction.Builder oMultiTransaction1 = MultiTransaction.newBuilder();
		MultiTransactionBody.Builder oMultiTransactionBody1 = MultiTransactionBody.newBuilder();
		MultiTransaction.Builder oMultiTransaction2 = MultiTransaction.newBuilder();
		MultiTransactionBody.Builder oMultiTransactionBody2 = MultiTransactionBody.newBuilder();

		
		MultiTransactionInput.Builder oMultiTransactionInput1 = MultiTransactionInput.newBuilder();
		
		oMultiTransactionInput1.setAddress(ByteString.copyFrom(oKeyPairs1.getAddress().getBytes()));
		oMultiTransactionInput1.setAmount(16);
		oMultiTransactionInput1.setToken("ABC");
		oMultiTransactionInput1.setFee(0);
		oMultiTransactionInput1.setFeeLimit(0);
		oMultiTransactionInput1.setNonce(0);
		oMultiTransactionBody1.addInputs(oMultiTransactionInput1);

		MultiTransactionOutput.Builder oMultiTransactionOutput1 = MultiTransactionOutput.newBuilder();
		oMultiTransactionOutput1.setAddress(ByteString.copyFrom(oKeyPairs2.getAddress().getBytes()));
		oMultiTransactionOutput1.setAmount(16);
		oMultiTransactionBody1.addOutputs(oMultiTransactionOutput1);

		MultiTransactionInput.Builder oMultiTransactionInput2 = MultiTransactionInput.newBuilder();
		oMultiTransactionInput2.setAddress(ByteString.copyFrom(oKeyPairs2.getAddress().getBytes()));
		oMultiTransactionInput2.setAmount(17);
		oMultiTransactionInput2.setToken("DEF");
		oMultiTransactionInput2.setFee(0);
		oMultiTransactionInput2.setFeeLimit(0);
		oMultiTransactionInput2.setNonce(0);
		oMultiTransactionBody2.addInputs(oMultiTransactionInput2);

		MultiTransactionOutput.Builder oMultiTransactionOutput2 = MultiTransactionOutput.newBuilder();
		oMultiTransactionOutput2.setAddress(ByteString.copyFrom(oKeyPairs1.getAddress().getBytes()));
		oMultiTransactionOutput2.setAmount(17);
		oMultiTransactionBody2.addOutputs(oMultiTransactionOutput2);

		oMultiTransactionBody1.setData(ByteString.copyFromUtf8("02"));
		oMultiTransactionBody2.setData(ByteString.copyFromUtf8("02"));

		oMultiTransaction1.setTxHash(ByteString.EMPTY);
		oMultiTransactionBody1.clearSignatures();
		oMultiTransaction2.setTxHash(ByteString.EMPTY);
		oMultiTransactionBody2.clearSignatures();

		// 签名
		MultiTransactionSignature.Builder oMultiTransactionSignature1 = MultiTransactionSignature.newBuilder();
		oMultiTransactionSignature1.setPubKey(oKeyPairs1.getPubkey());
		oMultiTransactionSignature1.setSignature(
				encApi.hexEnc(encApi.ecSign(oKeyPairs1.getPrikey(), oMultiTransaction1.build().toByteArray())));
		oMultiTransactionBody1.addSignatures(oMultiTransactionSignature1);

		MultiTransactionSignature.Builder oMultiTransactionSignature2 = MultiTransactionSignature.newBuilder();
		oMultiTransactionSignature1.setPubKey(oKeyPairs2.getPubkey());
		oMultiTransactionSignature1.setSignature(
				encApi.hexEnc(encApi.ecSign(oKeyPairs2.getPrikey(), oMultiTransaction2.build().toByteArray())));
		oMultiTransactionBody2.addSignatures(oMultiTransactionSignature2);

		try {
			// 测试其他节点，删除多重签名账户
			log.debug(String.format("账户1 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));
			oMultiTransaction1.setTxBody(oMultiTransactionBody1);
			//oMultiTransaction1.setTxBody(oMultiTransactionBody2);

			transactionHelper.CreateMultiTransaction(oMultiTransaction1);
			//transactionHelper.CreateMultiTransaction(oMultiTransaction2);

			log.debug(String.format("账户1 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BlockEntity.Builder oSyncBlock = BlockEntity.newBuilder();
		BlockEntity.Builder newBlock;
		try {
			newBlock = blockHelper.CreateNewBlock(600, ByteUtil.EMPTY_BYTE_ARRAY,
					ByteString.copyFromUtf8(coinBase).toByteArray());
			oSyncBlock.setHeader(newBlock.getHeader());
			blockHelper.ApplyBlock(oSyncBlock.build());
			log.debug("block已同步");

			log.debug(String.format("账户1 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 ABC %s DEF %s",
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "ABC"),
					accountHelper.getTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		//
		// // 创建多重签名交易
		// MultiTransaction.Builder oMultiTransaction2 =
		// MultiTransaction.newBuilder();
		// MultiTransactionInput.Builder oMultiTransactionInput4 =
		// MultiTransactionInput.newBuilder();
		// oMultiTransactionInput4.setAddress(ByteString.copyFrom(mAddress));
		// oMultiTransactionInput4.setAmount(12);
		// oMultiTransactionInput4.setFee(0);
		// oMultiTransactionInput4.setFeeLimit(0);
		// oMultiTransactionInput4.setNonce(0);
		// oMultiTransaction2.addInputs(oMultiTransactionInput4);
		//
		// // MultiTransactionOutput.Builder oMultiTransactionOutput =
		// // MultiTransactionOutput.newBuilder();
		// oMultiTransaction2.setData(ByteString.copyFromUtf8("01"));
		// oMultiTransaction2.setExdata(oAccount.toByteString());
		// oMultiTransaction2.setTxHash(ByteString.EMPTY);
		// oMultiTransaction2.clearSignatures();
		//
		// // 签名
		// MultiTransactionSignature.Builder oMultiTransactionSignature21 =
		// MultiTransactionSignature.newBuilder();
		// oMultiTransactionSignature21.setPubKey(oKeyPairs1.getPubkey());
		// oMultiTransactionSignature21.setSignature(
		// encApi.hexEnc(encApi.ecSign(oKeyPairs1.getPrikey(),
		// oMultiTransaction.build().toByteArray())));
		// oMultiTransaction2.addSignatures(oMultiTransactionSignature21);
		//
		// MultiTransactionSignature.Builder oMultiTransactionSignature22 =
		// MultiTransactionSignature.newBuilder();
		// oMultiTransactionSignature22.setPubKey(oKeyPairs2.getPubkey());
		// oMultiTransactionSignature22.setSignature(
		// encApi.hexEnc(encApi.ecSign(oKeyPairs2.getPrikey(),
		// oMultiTransaction.build().toByteArray())));
		// oMultiTransaction2.addSignatures(oMultiTransactionSignature22);
		//
		// MultiTransactionSignature.Builder oMultiTransactionSignature23 =
		// MultiTransactionSignature.newBuilder();
		// oMultiTransactionSignature23.setPubKey(oKeyPairs3.getPubkey());
		// oMultiTransactionSignature23.setSignature(
		// encApi.hexEnc(encApi.ecSign(oKeyPairs3.getPrikey(),
		// oMultiTransaction.build().toByteArray())));
		// oMultiTransaction2.addSignatures(oMultiTransactionSignature23);

		oRespTxTest.setRetCode(-1);
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespTxTest.build()));
	}
}
