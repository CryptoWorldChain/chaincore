package org.brewchain.account.test;

import java.util.Date;
import java.util.LinkedList;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.AccountCryptoToken;
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
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class CryptoTokenTest extends SessionModules<ReqTxTest> implements ActorService {
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
		return new String[] { PTSTCommand.CTT.name() };
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
		AccountCryptoToken.Builder oAccountCryptoToken1 = AccountCryptoToken.newBuilder();
		oAccountCryptoToken1.setCode("112");
		oAccountCryptoToken1.setIndex(1);
		oAccountCryptoToken1.setName("aab");
		oAccountCryptoToken1.setTimestamp(new Date().getTime());
		oAccountCryptoToken1.setTotal(100);
		oAccountCryptoToken1
				.setHash(ByteString.copyFrom(encApi.sha256Encode(oAccountCryptoToken1.build().toByteArray())));
		// 增加账户余额1
		try {

			accountHelper.addCryptoBalance(oKeyPairs1.getAddress().getBytes(), "DEF", oAccountCryptoToken1);
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
			AccountCryptoToken.Builder oAccountCryptoToken2 = AccountCryptoToken.newBuilder();
			oAccountCryptoToken2.setCode("111");
			oAccountCryptoToken2.setIndex(0);
			oAccountCryptoToken2.setName("aaa");
			oAccountCryptoToken2.setTimestamp(new Date().getTime());
			oAccountCryptoToken2.setTotal(100);
			oAccountCryptoToken2
					.setHash(ByteString.copyFrom(encApi.sha256Encode(oAccountCryptoToken2.build().toByteArray())));
			accountHelper.addCryptoBalance(oKeyPairs2.getAddress().getBytes(), "DEF", oAccountCryptoToken2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MultiTransaction.Builder oMultiTransaction1 = MultiTransaction.newBuilder();
		MultiTransactionBody.Builder oMultiTransactionBody1 = MultiTransactionBody.newBuilder();

		MultiTransactionInput.Builder oMultiTransactionInput1 = MultiTransactionInput.newBuilder();
		oMultiTransactionInput1.setAddress(ByteString.copyFrom(oKeyPairs1.getAddress().getBytes()));
		oMultiTransactionInput1.setAmount(16);
		oMultiTransactionInput1.setFee(0);
		oMultiTransactionInput1.setFeeLimit(0);
		oMultiTransactionInput1.setNonce(0);
		oMultiTransactionInput1.setCryptoToken(oAccountCryptoToken1.getHash());
		oMultiTransactionInput1.setSymbol("DEF");
		oMultiTransactionBody1.addInputs(oMultiTransactionInput1);

		MultiTransactionOutput.Builder oMultiTransactionOutput1 = MultiTransactionOutput.newBuilder();
		oMultiTransactionOutput1.setAddress(ByteString.copyFrom(oKeyPairs2.getAddress().getBytes()));
		oMultiTransactionOutput1.setCryptoToken(oAccountCryptoToken1.getHash());
		oMultiTransactionOutput1.setSymbol("DEF");
		oMultiTransactionBody1.addOutputs(oMultiTransactionOutput1);

		MultiTransactionSignature.Builder oMultiTransactionSignature1 = MultiTransactionSignature.newBuilder();
		oMultiTransactionSignature1.setPubKey(oKeyPairs1.getPubkey());
		oMultiTransactionSignature1.setSignature(
				encApi.hexEnc(encApi.ecSign(oKeyPairs1.getPrikey(), oMultiTransaction1.build().toByteArray())));
		oMultiTransactionBody1.addSignatures(oMultiTransactionSignature1);

		oMultiTransactionBody1.setData(ByteString.copyFromUtf8("05"));
		oMultiTransaction1.setTxBody(oMultiTransactionBody1);

		try {
			// 测试其他节点，删除多重签名账户
			log.debug(String.format("账户1 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));

			transactionHelper.CreateMultiTransaction(oMultiTransaction1);

			log.debug(String.format("账户1 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));

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

			log.debug(String.format("账户1 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs1.getAddress().getBytes(), "DEF")));
			log.debug(String.format("账户2 DEF %s",
					accountHelper.getCryptoTokenBalance(oKeyPairs2.getAddress().getBytes(), "DEF")));

		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
}
