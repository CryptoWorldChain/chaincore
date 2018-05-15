package org.brewchain.account;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.collections.functors.WhileClosure;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.account.core.BlockChainHelper;
import org.brewchain.account.core.KeyConstant;
import org.brewchain.account.dao.DefDaos;
import org.brewchain.account.gens.Actimpl.PACTCommand;
import org.brewchain.account.gens.Actimpl.PACTModule;
import org.brewchain.account.gens.Actimpl.ReqCreateAccount;
import org.fc.brewchain.p22p.node.Network;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class ApplicationStart extends SessionModules<Message> {

	@Override
	public String[] getCmds() {
		return new String[] { "___" };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	@ActorRequire(name = "BlockChain_Helper", scope = "global")
	BlockChainHelper blockChainHelper;
	@ActorRequire(name = "Def_Daos", scope = "global")
	DefDaos dao;

	@Validate
	public void startup() {
		try {
			new Thread(new AccountStartThread()).start();
//			final Timer timer = new Timer();
//			timer.schedule(new TimerTask() {
//				@Override
//				public void run() {
//					// copy db is db is not exists
//
//					// load block
//					blockChainHelper.onStart();
//
//					// get node
//					// Network oNetwork = dao.getPzp().networkByID("raft");
//					// KeyConstant.nodeName = oNetwork.root().name();
//					KeyConstant.nodeName = "测试节点01";
//				}
//			}, 1000 * 20);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class AccountStartThread extends Thread {
		@Override
		public void run() {
			try {
				while (dao == null || !dao.isReady()) {
					log.debug("等待dao注入完成...");
					Thread.sleep(1000);
				}
				log.debug("dao注入完成，开始加载block");
				// 如果是第一次启动，创建db目录
				
				log.debug("导入数据库");
				blockChainHelper.onStart();
				KeyConstant.nodeName = "测试节点01";
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
