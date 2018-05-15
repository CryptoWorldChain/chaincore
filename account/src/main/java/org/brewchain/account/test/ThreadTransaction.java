package org.brewchain.account.test;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadTransaction extends Thread {

	private AccountHelper accountHelper;
	private TransactionHelper transactionHelper;
	private List<KeyPairs> keys;
	private int maxKeys = 0;
	private Random random = new Random();
	private EncAPI encApi;
	private static int count = 0;

	public ThreadTransaction(AccountHelper accountHelper, TransactionHelper transactionHelper, EncAPI encApi,
			List<KeyPairs> keys) {
		this.accountHelper = accountHelper;
		this.transactionHelper = transactionHelper;
		this.keys = keys;
		maxKeys = keys.size();
		this.encApi = encApi;
	}

	@Override
	public void run() {
		final Timer timer = new Timer();
		// 设定定时任务
		timer.schedule(new TimerTask() {
			// 定时任务执行方法
			@Override
			public void run() {
				try {
					// 随意抽取两个用户，相互之间发送随机大小的交易
					int rKeys1 = random.nextInt(maxKeys - 1);
					int rKeys2 = random.nextInt(maxKeys - 1);
					int rAmount = random.nextInt(1000);

					KeyPairs oKeyPairs1 = keys.get(rKeys1);
					KeyPairs oKeyPairs2 = keys.get(rKeys2);

					int nonce = accountHelper.getNonce(encApi.hexDec(oKeyPairs1.getAddress()));

					MultiTransaction.Builder oMultiTransaction = MultiTransaction.newBuilder();
					MultiTransactionBody.Builder oMultiTransactionBody = MultiTransactionBody.newBuilder();
					MultiTransactionInput.Builder oMultiTransactionInput4 = MultiTransactionInput.newBuilder();
					oMultiTransactionInput4.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairs1.getAddress())));
					oMultiTransactionInput4.setAmount(rAmount);
					oMultiTransactionInput4.setFee(0);
					oMultiTransactionInput4.setFeeLimit(0);
					oMultiTransactionInput4.setNonce(nonce);
					oMultiTransactionBody.addInputs(oMultiTransactionInput4);

					MultiTransactionOutput.Builder oMultiTransactionOutput1 = MultiTransactionOutput.newBuilder();
					oMultiTransactionOutput1.setAddress(ByteString.copyFrom(encApi.hexDec(oKeyPairs2.getAddress())));
					oMultiTransactionOutput1.setAmount(rAmount);
					oMultiTransactionBody.addOutputs(oMultiTransactionOutput1);

					// MultiTransactionOutput.Builder oMultiTransactionOutput =
					// MultiTransactionOutput.newBuilder();
					oMultiTransactionBody.setData(ByteString.EMPTY);
					oMultiTransaction.setTxHash(ByteString.EMPTY);
					oMultiTransactionBody.clearSignatures();
					
					oMultiTransactionBody.setTimestamp((new Date()).getTime());
					// 签名
					MultiTransactionSignature.Builder oMultiTransactionSignature21 = MultiTransactionSignature
							.newBuilder();
					oMultiTransactionSignature21.setPubKey(oKeyPairs1.getPubkey());
					oMultiTransactionSignature21.setSignature(encApi
							.hexEnc(encApi.ecSign(oKeyPairs1.getPrikey(), oMultiTransactionBody.build().toByteArray())));
					oMultiTransactionBody.addSignatures(oMultiTransactionSignature21);

					oMultiTransaction.setTxBody(oMultiTransactionBody);
					transactionHelper.CreateMultiTransaction(oMultiTransaction);
					// log.debug(String.format("=====> 创建交易 %s 次数 %s 金额 %s 累计执行 %s", oKeyPairs1.getAddress(), nonce, rAmount, count));
					count += 1;
				} catch (Exception e) {
					e.printStackTrace();
					log.debug(String.format("=====> 执行 %s 交易异常 %s", count, e.getMessage()));
				}
			}
		}, 0, 1000);
	}
}
