package org.brewchain.account.test;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.gens.TxTest.RespTxTest;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadSignature extends Thread {
	private int count = 0;
	private EncAPI encApi;

	public ThreadSignature(int count, EncAPI encApi) {
		this.count = count;
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
				RespTxTest.Builder oRespTxTest = RespTxTest.newBuilder();
				Random r = new Random();
				oRespTxTest.setRetCode(r.nextInt(5));

				log.debug(String.format("%s %s", count,
						encApi.hexEnc(encApi.sha256Encode(oRespTxTest.build().toByteArray()))));
			}
		}, 0, 10);
	}
}
