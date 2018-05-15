package org.brewchain.evm.service;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.cvm.pbgens.Cvm.PCommand;
import org.brewchain.cvm.pbgens.Cvm.PMFunPut;
import org.brewchain.cvm.pbgens.Cvm.PModule;
import org.brewchain.cvm.pbgens.Cvm.PRetRun;
import org.brewchain.cvm.pbgens.Cvm.PSRunFun;
import org.brewchain.evm.base.MTransaction;
import org.brewchain.evm.call.CallTransaction;
import org.fc.brewchain.bcapi.EncAPI;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Data
@Slf4j
public class RunFunService extends SessionModules<PSRunFun> {

	@ActorRequire(name = "bc_encoder",scope = "global")
	EncAPI encAPI;

//	@ActorRequire(name = "Transaction_Helper", scope = "global")
//	TransactionHelper transactionHelper;
	@ActorRequire(name = "Account_Helper", scope = "global")
	AccountHelper accountHelper;
	
	@Override
	public String getModule() {
		return PModule.CVM.name();
	}

	@Override
	public String[] getCmds() {
		return new String[] { PCommand.RUF.name() };
	}

	public String toString() {
		return "RunFunService";
	}

	@Override
	public void onPBPacket(FramePacket pack, PSRunFun pbo, CompleteHandler handler) {
		final PRetRun.Builder ret = PRetRun.newBuilder();
		try {
			checkNull(pbo);
			
			
			String funcJson = "{ \n" ;
			
			if(pbo.getConstant()) {
				funcJson += "  'constant': true, \n";
			}else {
				funcJson += "  'constant': false, \n";
			}
			//{'name':'to', 'type':'address'}
			
			funcJson += "  'inputs': [\n";
			if(pbo.getInputsList() != null) {
				for(PMFunPut put : pbo.getInputsList()) {
					if(StringUtils.isNotBlank(put.getName()) || StringUtils.isNotBlank(put.getType())) {
						funcJson += "{'name':'"+put.getName()+"', 'type':'"+put.getType()+"'}\n";
					}
				}
			}
			funcJson +=  "], \n";
			
			funcJson += "  'outputs': [\n";
			if(pbo.getOutputsList() != null) {
				for(PMFunPut put : pbo.getOutputsList()) {
					if(StringUtils.isNotBlank(put.getName()) || StringUtils.isNotBlank(put.getType())) {
						funcJson += "{'name':'"+put.getName()+"', 'type':'"+put.getType()+"'}\n";
					}
				}
			}	
			funcJson += "], \n";
			
			funcJson += "  'name': '"+pbo.getName()+"', \n";
			if(StringUtils.isBlank(pbo.getType())) {
				funcJson += "  'type': 'function' \n";
			}else {
				funcJson += "  'type': '"+pbo.getType()+"' \n";
			}
			funcJson += "} \n";
			
			funcJson = funcJson.replaceAll("'", "\"");
			
			log.info("funcJson="+funcJson);
			
			long fee = 0L;
			long feeLimit = 0L;
			if(pbo.getFee() > 0) {
				fee = pbo.getFee();
				feeLimit = fee;
				if(pbo.getFeeLimit() > 0) {
					feeLimit = pbo.getFeeLimit  ();
				}
			}
			
			CallTransaction.Function function = CallTransaction.Function.fromJsonInterface(funcJson);
			
//			Object ... funcArgs
//			byte[] callData = function.encode(funcArgs);
			
//			MultiTransaction.Builder ctx = CallTransaction.createCallTransaction(
//														1L, pbo.getFee(), pbo.getFromAddr(), pbo.getPubKey()
//														,pbo.getToAddr(), 0
//														, pbo.getCtxSignBytes().toString(), function).toBuilder();
			

			MTransaction tx = new MTransaction(accountHelper);
			tx.addTXInput(pbo.getFromAddr(), pbo.getPubKey(), pbo.getCtxSignBytes().toString(), 0L, fee, feeLimit);
			tx.addTXOutput(pbo.getToAddr(), 0L);
			MultiTransaction.Builder ctx = tx.genTX(null, null);
			
			
			ret.setRetCode(0);
			ret.setRetMessage("");
			ret.setRunInfo(encAPI.hexEnc(ctx.getTxBody().getData().toByteArray()));
	        
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

	public void checkNull(PSRunFun pb) {
		if (pb == null) {
			throw new IllegalArgumentException("无请求参数");
		}

		if (StringUtils.isBlank(pb.getName())) {
			throw new IllegalArgumentException("参数name,不能为空");
		}

//		if (pb.getInputsList() == null || pb.getInputsList().size() == 0) {
//			throw new IllegalArgumentException("参数inputs,不能为空");
//		}
//		
//		if (pb.getOutputsList() == null || pb.getOutputsList().size() == 0) {
//			throw new IllegalArgumentException("参数outputs,不能为空");
//		}

		if (StringUtils.isBlank(pb.getToAddr())) {
			throw new IllegalArgumentException("参数to_addr,不能为空");
		}
		
		if (StringUtils.isBlank(pb.getCtxSign())) {
			throw new IllegalArgumentException("参数ctx_sign,不能为空");
		}
		
		
//		if (pb.getGasPrice() <= 0) {
//			throw new IllegalArgumentException("参数gas_price,不能为空");
//		}
		
		if (StringUtils.isBlank(pb.getFromAddr())) {
			throw new IllegalArgumentException("参数from_addr,不能为空");
		}
		if (StringUtils.isBlank(pb.getPubKey())) {
			throw new IllegalArgumentException("参数pub_key,不能为空");
		}
	}
}
