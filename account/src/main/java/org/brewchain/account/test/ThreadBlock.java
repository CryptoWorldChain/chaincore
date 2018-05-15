package org.brewchain.account.test;

import java.util.Timer;
import java.util.TimerTask;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.gens.Block.BlockEntity;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadBlock extends Thread {
	private BlockHelper blockHelper;
	private static int count = 0;
	private EncAPI encApi;

	public ThreadBlock(BlockHelper blockHelper, EncAPI encApi) {
		this.blockHelper = blockHelper;
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
				BlockEntity.Builder oSyncBlock = BlockEntity.newBuilder();
				BlockEntity.Builder newBlock;
				try {
					newBlock = blockHelper.CreateNewBlock(600, ByteUtil.EMPTY_BYTE_ARRAY,
							ByteString.copyFromUtf8("12345").toByteArray());
					oSyncBlock.setHeader(newBlock.getHeader());
					//oSyncBlock.setBody(newBlock.getBody());
					log.debug(String.format("==> 第 %s 块 hash %s 创建成功", oSyncBlock.getHeader().getNumber(),
							encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray())));
					blockHelper.ApplyBlock(oSyncBlock.build());
					log.debug(String.format("==> 第 %s 块 hash %s 父hash %s 交易 %s 笔", oSyncBlock.getHeader().getNumber(),
							encApi.hexEnc(oSyncBlock.getHeader().getBlockHash().toByteArray()),
							encApi.hexEnc(oSyncBlock.getHeader().getParentHash().toByteArray()),
							oSyncBlock.getHeader().getTxHashsCount()));
					count += 1;
				} catch (Exception e) {
					e.printStackTrace();
					log.debug(String.format("执行 %s 区块异常 %s", count, e.getMessage()));
				}
			}
		}, 0, 1000 * 5);
	}
}
