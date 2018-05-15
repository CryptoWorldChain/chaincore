package org.brewchain.account.core;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.account.dao.DefDaos;
import org.brewchain.account.gens.Actimpl.PACTModule;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;

@NActorProvider
@Data
@Slf4j
@Instantiate(name = "BlockChain_Config")
public class BlockChainConfig extends SessionModules<Message> {
	private String coinBase = props().get("block.coinBase.hex", null);
	private int minerReward = props().get("block.miner.reward", 0);
	private int minerRewardWait = props().get("block.miner.reward.wait", 0);

	@Override
	public String[] getCmds() {
		return new String[] { "BlockChainConfig" };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@Override
	public void onDaoServiceAllReady() {
		// 校验配置是否有效
		log.debug(String.format("配置 %s = %s", "block.coinBase.hex", coinBase));
		log.debug(String.format("配置 %s = %s", "block.miner.reward", minerReward));
	}
}
