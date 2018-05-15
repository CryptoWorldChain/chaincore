package org.brewchain.evm.base;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionBody;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;

public class MTransaction {
	
	private MultiTransaction.Builder tx;
	private MultiTransactionBody.Builder txBody = MultiTransactionBody.newBuilder();
	private String token;
	
	private AccountHelper accountHelper;
	
	public MTransaction(AccountHelper accountHelper) {
		
		this.tx = MultiTransaction.newBuilder();
		this.tx.setTxHash(ByteString.EMPTY);
		this.txBody = MultiTransactionBody.newBuilder();
		this.txBody.clearSignatures();
		
		this.accountHelper = accountHelper;
	}

	public MTransaction(String token,AccountHelper accountHelper) {
		
		this.token = token;
		
		this.tx = MultiTransaction.newBuilder();
		this.tx.setTxHash(ByteString.EMPTY);
		this.txBody = MultiTransactionBody.newBuilder();
		this.txBody.clearSignatures();
		
		this.accountHelper = accountHelper;
	}
	
	public void addTXInput(String address, String pubKey, String sign, long value, long fee, long feeLimit) throws Exception {
		
		MultiTransactionInput.Builder txInput = MultiTransactionInput.newBuilder();
		txInput.setAddress(ByteString.copyFrom(Hex.decode(address)));
		txInput.setAmount(value);
		txInput.setFee((int)fee);
		txInput.setFeeLimit((int)feeLimit);
		txInput.setNonce(accountHelper.getNonce(Hex.decode(address)));
		if(StringUtils.isNotBlank(token)) txInput.setToken(token);
		this.txBody.addInputs(txInput);
		
		// 签名
		MultiTransactionSignature.Builder signature = MultiTransactionSignature.newBuilder();
		signature.setPubKey(pubKey);
		signature.setSignature(sign);
		this.txBody.addSignatures(signature);
		
    }
	
    public void addTXOutput(String address, long value) {
    		if(StringUtils.isNotBlank(address)) {
    			//合约时不能addOutputs
	    		MultiTransactionOutput.Builder TXOutput = MultiTransactionOutput.newBuilder();
	    		TXOutput.setAddress(ByteString.copyFrom(Hex.decode(address)));
	    		TXOutput.setAmount(value);
	    		this.txBody.addOutputs(TXOutput);
    		}
    }
    
    public MultiTransaction.Builder genTX(byte[] data, byte[] exData) {
    		if(data != null) this.txBody.setData(ByteString.copyFrom(data));
    		if(exData != null) this.txBody.setExdata(ByteString.copyFrom(exData));
    		if(StringUtils.isNotBlank(token)) this.txBody.setData(ByteString.copyFromUtf8("02"));
		this.tx.setTxBody(this.txBody);
		return tx;
    }
    
    public void sendTX(TransactionHelper transactionHelper,byte[] data, byte[] exData) throws Exception {
//		if(txBody.getInputsList().size() == 0) {
//			//合约交易，无addOutputs
//		}else if(StringUtils.isNotBlank(token)){
//			// erc20交易
//		}else {
//		}
		transactionHelper.CreateMultiTransaction(this.genTX(data, exData));	
    }
    
    public Account getAccount(TransactionHelper transactionHelper, String addr) {
    		Account account = accountHelper.GetAccount(Hex.decode(addr));
    		
    		return account;
    }
	
}
