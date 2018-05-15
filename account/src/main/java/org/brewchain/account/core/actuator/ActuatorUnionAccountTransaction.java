package org.brewchain.account.core.actuator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.brewchain.account.core.AbstractLocalCache;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransaction.Builder;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionSignature;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ActuatorUnionAccountTransaction extends AbstractTransactionActuator implements iTransactionActuator {

	public ActuatorUnionAccountTransaction(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper,
			BlockHelper oBlockHelper, EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
	}

	@Override
	public void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		// 校验每日最大转账金额
		// 校验每笔最大转账金额
		// 校验超过单笔转账金额后的用户签名
		if (senders.size() != 1) {
			throw new Exception(String.format("发送方地址不存在或者出现多个发送方地址 %s", senders.size()));
		}

		AccountValue.Builder oSenderValue = senders.get(senders.keySet().toArray()[0]).getValue().toBuilder();

		long totalAmount = 0;
		for (MultiTransactionInput oInput : oMultiTransaction.getTxBody().getInputsList()) {
			totalAmount += oInput.getAmount();
			totalAmount += oInput.getFee();
		}
		String key = String.format("%s_%s", new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
				encApi.hexEnc(oMultiTransaction.getTxBody().getInputs(0).getAddress().toByteArray()));
		log.debug(String.format("%s 累计 %s", key, AbstractLocalCache.dayTotalAmount.get(key)));

		long dayTotal = totalAmount + AbstractLocalCache.dayTotalAmount.get(key);
		long dayMax = oSenderValue.getMax();
		if (dayTotal > dayMax) {
			throw new Exception(String.format("账户当天的累计交易金额 %s 超过单日最大交易限额 %s", dayTotal, dayMax));
		}
		// 当单笔金额超过一个预设值后，则需要多方签名
		if (totalAmount > oSenderValue.getAcceptMax()) {
			if (oMultiTransaction.getTxBody().getSignaturesCount() != oSenderValue.getAcceptMax()) {
				throw new Exception(String.format("当前的交易金额 %s 大于 %s 时需要 %s 方签名才可以发起交易", totalAmount,
						oSenderValue.getAcceptMax(), oSenderValue.getAcceptLimit()));
			} else {
				// TODO 如何判断交易的签名，是由多重签名账户的关联账户进行签名的
			}
		} else {
			// 需要至少有一个子账户签名
			if (oMultiTransaction.getTxBody().getSignaturesCount() == 0) {
				throw new Exception(String.format("交易需要至少一个签名才能被验证"));
			}
		}

		super.onPrepareExecute(oMultiTransaction, senders, receivers);
	}

	@Override
	public void onExecuteDone(MultiTransaction oMultiTransaction) throws Exception {
		// 缓存，累加当天转账金额
		long totalAmount = 0;
		for (MultiTransactionInput oInput : oMultiTransaction.getTxBody().getInputsList()) {
			totalAmount += oInput.getAmount();
			totalAmount += oInput.getFee();
		}
		String key = String.format("%s_%s", new SimpleDateFormat("yyyy-MM-dd").format(new Date()),
				encApi.hexEnc(oMultiTransaction.getTxBody().getInputs(0).getAddress().toByteArray()));
		long v = AbstractLocalCache.dayTotalAmount.get(key);
		AbstractLocalCache.dayTotalAmount.put(key, v + totalAmount);
		super.onExecuteDone(oMultiTransaction);
	}

	@Override
	public void onVerifySignature(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {

		// 签名的账户是否是该多重签名账户的子账户，如果不是，抛出异常
		for (MultiTransactionSignature oSignature : oMultiTransaction.getTxBody().getSignaturesList()) {
			// TODO 需要能解出签名地址的方法，验证每个签名地址都是多重签名账户的关联自账户
		}

		super.onVerifySignature(oMultiTransaction, senders, receivers);
	}
}
