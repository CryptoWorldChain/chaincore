package org.brewchain.account.test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockChainHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
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
public class BlockSingleTest extends SessionModules<ReqTxTest> implements ActorService {
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
		return new String[] { PTSTCommand.BST.name() };
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

		} catch (Exception e) {
			// TODO: handle exception
		}
		BlockEntity.Builder oBlockEntity;
		try {
			oBlockEntity = blockHelper.GetBestBlock();
		} catch (Exception e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		// 创建账户1
		KeyPairs oKeyPairs1 = encApi.genKeys();
		// 创建账户1
		accountHelper.CreateAccount(oKeyPairs1.getAddress().getBytes(), oKeyPairs1.getPubkey().getBytes());
		// 增加账户余额1
		try {
			accountHelper.addBalance(oKeyPairs1.getAddress().getBytes(), 10000000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 创建账户2
		KeyPairs oKeyPairs2 = encApi.genKeys();
		// 创建账户1
		accountHelper.CreateAccount(oKeyPairs2.getAddress().getBytes(), oKeyPairs2.getPubkey().getBytes());
		// 增加账户余额1
		try {
			accountHelper.addBalance(oKeyPairs2.getAddress().getBytes(), 10000000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			log.debug(String.format("账户1余额 %s 账户2余额 %s", accountHelper.getBalance(oKeyPairs1.getAddress().getBytes()),
					accountHelper.getBalance(oKeyPairs2.getAddress().getBytes())));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// 发送交易
		// 模拟合约交易
		// 1. 签名
		// 2. 生成Hash
		// 3. 生成Sender
		SingleTransaction.Builder newTx = SingleTransaction.newBuilder();
		newTx.setAmount(98);
		newTx.setData(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		newTx.setExdata(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		newTx.setFee(0);
		newTx.setFeeLimit(2);
		try {
			newTx.setNonce(accountHelper.getNonce(oKeyPairs2.getAddress().getBytes()));
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		newTx.setPubKey(oKeyPairs2.getPubkey());
		newTx.setReceiveAddress(ByteString.copyFrom(oKeyPairs1.getAddress().getBytes()));
		newTx.setSenderAddress(ByteString.copyFrom(oKeyPairs2.getAddress().getBytes()));
		newTx.setTimestamp(new Date().getTime());
		newTx.setTxHash(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));

		List<String> privs = new LinkedList<String>();
		privs.add(oKeyPairs2.getPrikey());

		try {
			MultiTransaction.Builder oMultiTransaction = transactionHelper
					.ParseSingleTransactionToMultiTransaction(newTx);

			MultiTransactionSignature.Builder oMultiTransactionSignature = MultiTransactionSignature.newBuilder();
			oMultiTransactionSignature.setPubKey(oKeyPairs2.getPubkey());
			oMultiTransactionSignature.setSignature(
					encApi.hexEnc(encApi.ecSign(oKeyPairs2.getPrikey(), oMultiTransaction.build().toByteArray())));

			MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();

			oMultiTransactionBody.addSignatures(oMultiTransactionSignature);
			oMultiTransaction.setTxBody(oMultiTransactionBody);
			// transactionHelper.Signature(privs, oMultiTransaction);
			transactionHelper.CreateMultiTransaction(oMultiTransaction);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		log.debug("交易创建！ " + Hex.toHexString(newTx.getTxHash().toByteArray()));
		try {
			log.debug(String.format("账户1余额 %s 账户2余额 %s", accountHelper.getBalance(oKeyPairs1.getAddress().getBytes()),
					accountHelper.getBalance(oKeyPairs2.getAddress().getBytes())));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		BlockEntity.Builder oSyncBlock = BlockEntity.newBuilder();
		BlockEntity.Builder newBlock;
		try {
			newBlock = blockHelper.CreateNewBlock(600, ByteUtil.EMPTY_BYTE_ARRAY,
					ByteString.copyFromUtf8(coinBase).toByteArray());
			log.debug("创建区块 " + newBlock.toString());
			oSyncBlock.setHeader(newBlock.getHeader());
			blockHelper.ApplyBlock(oSyncBlock.build());
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		log.debug("block已同步");

		try {
			log.debug(String.format("账户1余额 %s 账户2余额 %s", accountHelper.getBalance(oKeyPairs1.getAddress().getBytes()),
					accountHelper.getBalance(oKeyPairs2.getAddress().getBytes())));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		oRespTxTest.setRetCode(-1);
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespTxTest.build()));
	}
}
