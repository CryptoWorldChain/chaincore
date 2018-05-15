package org.brewchain.zero.utils;
import java.util.Date;
import java.util.List;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Tx;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.brewchain.account.gens.Tx.SingleTransaction;
import org.brewchain.account.util.ByteUtil;
import org.brewchain.zero.pbgens.Zero.Output;
import org.brewchain.zero.pbgens.Zero.TXInfo;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import com.google.protobuf.ByteString;

import lombok.Data;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Data
@Instantiate(name = "account_Util")
public class AccountUtil implements ActorService {

	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;
	

	@ActorRequire(name="Account_Helper", scope = "global")
	AccountHelper accountHelper;
	
	@ActorRequire(name = "Transaction_Helper", scope = "global")
	TransactionHelper transactionHelper;
	

	@ActorRequire(name = "bc_encoder",scope = "global")
	EncAPI encAPI;
	
	public TXInfo.Builder newAccount(String name, String type, int amount,String ext,List<org.brewchain.account.gens.Tx.MultiTransaction> ogtxList) throws Exception {
		TXInfo.Builder tx = TXInfo.newBuilder();
		Output.Builder output = Output.newBuilder();
		
		KeyPairs eckey = encAPI.genKeys();
		
		output.setBcuid(eckey.getBcuid());
		output.setPri(eckey.getPrikey());
		output.setPub(eckey.getPubkey());
		output.setAddress(eckey.getAddress());
//		output.setRipedmd160(encAPI.hexEnc(HashUtil.ripemd160(eckey.getAddress().getBytes())));
		output.setValue(amount);
		
		tx.addOutputs(output);
		
		
		// 创建账户
		accountHelper.CreateAccount(encAPI.hexDec(eckey.getAddress()) , eckey.getPubkey().getBytes());
		// 增加账户余额
		try {
			// erc20
			if("erc20".equals(type)) {
				// cws erc20
				accountHelper.addTokenBalance(encAPI.hexDec(eckey.getAddress()) , name, amount);
			}else {
				// cwc default
				accountHelper.addBalance(encAPI.hexDec(eckey.getAddress()) , amount);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// 创建创世块交易
		SingleTransaction.Builder newTx = SingleTransaction.newBuilder();
		newTx.setAmount(amount);
		newTx.setFee(0);
		newTx.setFeeLimit(0);
		newTx.setPubKey(eckey.getPubkey());
		
		// erc20
		if("erc20".equals(type)) {
			newTx.setToken(name);
			newTx.setData(ByteString.copyFromUtf8("02"));
		}
		
		newTx.setReceiveAddress(ByteString.copyFrom(encAPI.hexDec(eckey.getAddress()) ));
		newTx.setSenderAddress(ByteString.copyFrom(encAPI.hexDec(eckey.getAddress()) ));
		newTx.setTxHash(ByteString.EMPTY);
		newTx.setTimestamp(new Date().getTime());
		try {
			newTx.setNonce(accountHelper.getNonce(encAPI.hexDec(eckey.getAddress()) ));
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		
		newTx.setData(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		newTx.setExdata(ByteString.copyFrom(ext.getBytes()));

		// TODO new add
//		newTx.addDelegate(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY));
		
		newTx.clearSignature();
		
		try {
			
			Tx.MultiTransaction.Builder oMultiTransaction = transactionHelper
					.ParseSingleTransactionToMultiTransaction(newTx);
			
//			transactionHelper.Signature(privs, oMultiTransaction);
			
//			oMultiTransaction.clearSignatures();
			MultiTransactionSignature.Builder mts = MultiTransactionSignature.newBuilder();
			mts.setPubKey(eckey.getPubkey());
			mts.setSignature(encAPI.hexEnc(encAPI.ecSign(eckey.getPrikey(), oMultiTransaction.getTxBody().toByteArray())));
//			oMultiTransaction.addSignatures(mts);
			
			MultiTransactionBody.Builder b = oMultiTransaction.getTxBody().toBuilder();
			
			b.clearSignatures();
			b.addSignatures(mts);
			oMultiTransaction.setTxBody(b);
			
			transactionHelper.CreateMultiTransaction(oMultiTransaction);	
			
			ogtxList.add(oMultiTransaction.build());
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		tx.setNonce(newTx.getNonce());
		tx.setExt(ext);
		
		return tx;
	}
	
}
