package org.brewchain.account.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.bouncycastle.util.encoders.Hex;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.gens.Tx.SingleTransaction;
import org.brewchain.account.gens.TxTest.PTSTCommand;
import org.brewchain.account.gens.TxTest.PTSTModule;
import org.brewchain.account.gens.TxTest.ReqTTT;
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
public class ThreadTest extends SessionModules<ReqTTT> implements ActorService {
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
		return new String[] { PTSTCommand.TTT.name() };
	}

	@Override
	public String getModule() {
		return PTSTModule.TST.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqTTT pb, final CompleteHandler handler) {
		RespTxTest.Builder oRespTxTest = RespTxTest.newBuilder();
		oRespTxTest.setRetCode(1234);
		List<KeyPairs> listKeys = new ArrayList<KeyPairs>();
		if (pb.getBlock() == 1) {
			// 创建创世块
			try {
				blockHelper.CreateGenesisBlock(new LinkedList<MultiTransaction>(), ByteUtil.EMPTY_BYTE_ARRAY);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// 创建账户 100 个
		int accountCount = 100;
		while (accountCount > 0) {
			accountCount--;
			KeyPairs oKeyPairs = encApi.genKeys();
			accountHelper.CreateAccount(encApi.hexDec(oKeyPairs.getAddress()), encApi.hexDec(oKeyPairs.getPubkey()));
			listKeys.add(oKeyPairs);
			try {
				accountHelper.addBalance(encApi.hexDec(oKeyPairs.getAddress()), 10000000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			log.info(String.format("=====> 创建账户 %s", oKeyPairs.getAddress()));
		}

		try {
			// // 开始交易测试
			// ThreadTransaction oThreadTransaction = new
			// ThreadTransaction(accountHelper, transactionHelper, encApi,
			// listKeys);
			// oThreadTransaction.start();
			// //Thread.currentThread().sleep(50);
			// ThreadTransaction oThreadTransaction1 = new
			// ThreadTransaction(accountHelper, transactionHelper, encApi,
			// listKeys);
			// oThreadTransaction1.start();
			// //Thread.currentThread().sleep(60);
			// ThreadTransaction oThreadTransaction2 = new
			// ThreadTransaction(accountHelper, transactionHelper, encApi,
			// listKeys);
			// oThreadTransaction2.start();
			// //Thread.currentThread().sleep(110);
			// ThreadTransaction oThreadTransaction3 = new
			// ThreadTransaction(accountHelper, transactionHelper, encApi,
			// listKeys);
			// oThreadTransaction3.start();
			// //Thread.currentThread().sleep(160);
			// ThreadTransaction oThreadTransaction4 = new
			// ThreadTransaction(accountHelper, transactionHelper, encApi,
			// listKeys);
			// oThreadTransaction4.start();
			// //Thread.currentThread().sleep(310);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ThreadTransaction oThreadTransaction5 = new ThreadTransaction(accountHelper, transactionHelper, encApi,
				listKeys);
		oThreadTransaction5.start();

		// 开始打包
		ThreadBlock oThreadBlock = new ThreadBlock(blockHelper, encApi);
		oThreadBlock.start();

		oRespTxTest.setRetCode(-1);
		handler.onFinished(PacketHelper.toPBReturn(pack, oRespTxTest.build()));
	}
}
