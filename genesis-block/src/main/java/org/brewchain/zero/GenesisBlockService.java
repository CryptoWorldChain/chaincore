package org.brewchain.zero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.trie.TrieImpl;
import org.brewchain.zero.pbgens.Zero.PGBCommand;
import org.brewchain.zero.pbgens.Zero.PGBModule;
import org.brewchain.zero.pbgens.Zero.PRetGenesisBlock;
import org.brewchain.zero.pbgens.Zero.PSGenesisBlock;
import org.brewchain.zero.pbgens.Zero.RetInfo;
import org.brewchain.zero.pbgens.Zero.TXInfo;
import org.brewchain.zero.pbgens.Zero.TokenInfo;
import org.brewchain.zero.utils.AccountUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.UUIDGenerator;

@NActorProvider
@Data
@Slf4j
public class GenesisBlockService extends SessionModules<PSGenesisBlock> {

	@ActorRequire(name = "account_Util")
	@Getter
	@Setter
	AccountUtil accountUtil;

	@ActorRequire(name="Block_Helper", scope = "global")
	@Getter
	@Setter
	BlockHelper blockHelper;
	
	@ActorRequire(name = "bc_encoder",scope = "global")
	@Getter
	@Setter
	EncAPI encAPI;
	
	@Override
	public String getModule() {
		return PGBModule.GBN.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] { PGBCommand.GBG.name() };
	}
	
	
	
	public String toString() {
		return "GenesisBlockService";
	}

	@Override
	public void onPBPacket(FramePacket pack, PSGenesisBlock pbo, CompleteHandler handler) {
		final PRetGenesisBlock.Builder ret = PRetGenesisBlock.newBuilder();
		try {
			
			checkNull(pbo);
			//默认 100个地址
			int addr_count = 100;
			// 默认 每个地址设置1亿
			int addr_amount = 100_000_000;
			
			String name = "",type="",ext="",hosts = "",host="";
			List<String> list = new ArrayList<String>();
			TrieImpl receiptsTrie = new TrieImpl();
			LinkedList<MultiTransaction> ogtxList = new LinkedList<MultiTransaction>();
			for(TokenInfo t : pbo.getTokenList()) {
				list.clear();
				name = t.getName();
				type = t.getType();
				if(t.getAddrCount() > 0) {
					addr_count = t.getAddrCount();
				}
				if(t.getAddrAmount() > 0) {
					addr_amount = t.getAddrAmount();
				}
				if(t.getHostsList() != null && t.getHostsList().size() > 0) {
					if(t.getHostsList().size() != addr_count) {
						log.warn("["+t.getName()+"],地址个数["+addr_count+"],hosts个数["+t.getHostsList().size()+"],不匹配");
					}
					for(int i=0;i<t.getHostsList().size();i++) {
						list.add(t.getHostsList().get(i).getIp()+":"+t.getHostsList().get(i).getPort());
					}
					//1 hosts 排序
					Collections.sort(list);
					
					for(int i=0;i<list.size();i++) {
						hosts += "'"+list.get(i)+"',";
						log.info("host"+i+"="+list.get(i));
						//2 排序后，逐个put到MPT
						receiptsTrie.put(UUIDGenerator.generate().getBytes(), list.get(i).getBytes());
					}
					hosts = hosts.substring(0,hosts.length()-1);
				}
				

				RetInfo.Builder retinfo = RetInfo.newBuilder();
				retinfo.setTokenName(name);
				
				for(int i=0;i<addr_count;i++) {
					
					if(list.size() > i) {
						host = list.get(i);
					}else {
						host = "";
					}
					ext = "{'tn':'"+name+"','host':'"+host+"'}";
					ext = ext.replace("'", "\"");
					//创建账户、设置账户地址余额、记录设置账户地址余额交易
					TXInfo.Builder tx = accountUtil.newAccount(name,type,addr_amount,ext,ogtxList);
					if(tx != null) {
						retinfo.addTx(tx);
					}
				}

				ret.addToken(retinfo);
				
			}
			
			//3 获取 MPT Root hash
			String rootHash = encAPI.hexEnc(receiptsTrie.getRootHash());
			
			String bolckExt = "{'hosts_mpt_root_hash':'"+rootHash+"','hosts':["+hosts+"]}";

			log.info("ext====================="+ext);
			
			ext = ext.replace("'", "\"");
			
			ret.setBolckExt(bolckExt);
			
			//4 生成block
			log.info("######ogtxList.size######="+ogtxList.size());
			blockHelper.CreateGenesisBlock(ogtxList,bolckExt.getBytes());
			
			ret.setRetCode(0);
			ret.setRetMessage("");
			
		} catch (IllegalArgumentException e) {
			ret.setRetCode(-1);
			ret.setRetMessage(e.getMessage());
			log.error("error：：" + e.getMessage());
		} catch (UnknownError e) {
			ret.setRetCode(-1);
			ret.setRetMessage(e.getMessage());
			log.error("error：：" + e.getMessage());
		} catch (Exception e) {
			ret.setRetCode(-1);
			ret.setRetMessage(e.getMessage());
			log.error("error：：" + e.getMessage());
		} finally {

		}
		handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
	}
	
	public void checkNull(PSGenesisBlock pb) {
		if (pb == null) {
			throw new IllegalArgumentException("无请求参数");
		}
		if(pb.getTokenList() == null || pb.getTokenList().size() == 0) {
			throw new IllegalArgumentException("缺少token请求参数");
		}
		if (StringUtils.isBlank(pb.getTokenList().get(0).getName())) {
			throw new IllegalArgumentException("token中缺少name请求参数");
		}
	}
	
}
