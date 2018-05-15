package org.brewchain.account.function;

import org.apache.felix.ipojo.util.Log;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.test.InternalCallTest;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalFunction {

	/**
	 * 挖矿奖励
	 * 
	 * @param accountHelper
	 * @param blackHoleAddress
	 * @param coinBase
	 * @param balance
	 * @throws Exception
	 */
	public static void MinerReward(AccountHelper accountHelper, String... params) throws Exception {
		String blackHoleAddress = "";
		String balance = "0";
		String coinBase = "";
		log.debug(String.format("call MinerReward!"));
		accountHelper.addBalance(ByteString.copyFromUtf8(coinBase).toByteArray(), Long.valueOf(balance));
		// 黑洞地址
		accountHelper.addBalance(ByteString.copyFromUtf8(blackHoleAddress).toByteArray(), 0 - Long.valueOf(balance));
	}

	/**
	 * 挖矿惩罚
	 * 
	 * @param accountHelper
	 * @param blackHoleAddress
	 * @param coinBase
	 * @param balance
	 * @throws Exception
	 */
	public static void MinerPunish(AccountHelper accountHelper, byte[] blackHoleAddress, byte[] coinBase, long balance)
			throws Exception {
		accountHelper.addBalance(coinBase, 0 - balance);
		// 黑洞地址
		accountHelper.addBalance(blackHoleAddress, balance);
	}
}
