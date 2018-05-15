package org.brewchain.account.core.actuator;

import java.util.LinkedList;
import java.util.Map;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountTokenValue;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransaction.Builder;
import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.fc.brewchain.bcapi.EncAPI;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;

import com.google.protobuf.ByteString;

public class ActuatorTokenTransaction extends AbstractTransactionActuator implements iTransactionActuator {

	public ActuatorTokenTransaction(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper,
			BlockHelper oBlockHelper, EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
	}

	/*
	 * 校验交易有效性。余额，索引等
	 * 
	 * @see org.brewchain.account.core.transaction.AbstractTransactionActuator#
	 * onVerify(org.brewchain.account.gens.Tx.MultiTransaction, java.util.Map,
	 * java.util.Map)
	 */
	@Override
	public void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		// 交易中的Token必须一致
		String token = "";
		long inputsTotal = 0;
		long outputsTotal = 0;

		for (MultiTransactionInput oInput : oMultiTransaction.getTxBody().getInputsList()) {
			if (oInput.getToken().isEmpty() || oInput.getToken() == "") {
				throw new Exception(String.format("Token交易中Token不允许为空"));
			}
			if (token == "") {
				token = oInput.getToken();
			} else {
				if (!token.equals(oInput.getToken())) {
					throw new Exception(String.format("交易中不允许存在多个Token %s %s ", token, oInput.getToken()));
				}
			}

			// 取发送方账户
			Account sender = senders.get(oInput.getAddress());
			AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();
			long tokenBalance = 0;
			for (int i = 0; i < senderAccountValue.getTokensCount(); i++) {
				if (senderAccountValue.getTokens(i).getToken().equals(oInput.getToken())) {
					tokenBalance = senderAccountValue.getTokens(i).getBalance();
					break;
				}
			}

			inputsTotal += tokenBalance;

			if (tokenBalance - oInput.getAmount() - oInput.getFeeLimit() >= 0) {
				// 余额足够
			} else {
				throw new Exception(String.format("用户的账户余额 %s 不满足交易的最高限额 %s", tokenBalance,
						oInput.getAmount() + oInput.getFeeLimit()));
			}

			// 判断nonce是否一致
			int nonce = senderAccountValue.getNonce();
			if (nonce != oInput.getNonce()) {
				throw new Exception(String.format("用户的交易索引 %s 与交易的索引不一致 %s", nonce, oInput.getNonce()));
			}
		}

		for (MultiTransactionOutput oOutput : oMultiTransaction.getTxBody().getOutputsList()) {
			outputsTotal += oOutput.getAmount();
		}

		if (inputsTotal < outputsTotal) {
			throw new Exception(String.format("交易的输入 %S 小于输出 %s 金额", inputsTotal, outputsTotal));
		}
	}

	@Override
	public void onExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		LinkedList<OKey> keys = new LinkedList<OKey>();
		LinkedList<OValue> values = new LinkedList<OValue>();

		String token = "";
		for (MultiTransactionInput oInput : oMultiTransaction.getTxBody().getInputsList()) {
			// 取发送方账户
			Account sender = senders.get(oInput.getAddress());
			AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();

			token = oInput.getToken();
			boolean isExistToken = false;
			for (int i = 0; i < senderAccountValue.getTokensCount(); i++) {
				if (senderAccountValue.getTokens(i).getToken().equals(oInput.getToken())) {
					senderAccountValue.setTokens(i, senderAccountValue.getTokens(i).toBuilder().setBalance(
							senderAccountValue.getTokens(i).getBalance() - oInput.getAmount() - oInput.getFee()));
					isExistToken = true;
					break;
				}
			}
			if (!isExistToken) {
				throw new Exception(String.format("发送方账户异常，缺少token %s", oInput.getToken()));
			}

			// 不论任何交易类型，都默认执行账户余额的更改
			senderAccountValue.setBalance(senderAccountValue.getBalance() - oInput.getAmount() - oInput.getFee());
			senderAccountValue.setNonce(senderAccountValue.getNonce() + 1);
			
			keys.add(OEntityBuilder.byteKey2OKey(sender.getAddress().toByteArray()));
			values.add(OEntityBuilder.byteValue2OValue(senderAccountValue.build().toByteArray()));
			
		}

		for (MultiTransactionOutput oOutput : oMultiTransaction.getTxBody().getOutputsList()) {
			Account receiver = receivers.get(oOutput.getAddress());
			AccountValue.Builder receiverAccountValue = receiver.getValue().toBuilder();

			// 不论任何交易类型，都默认执行账户余额的更改
			receiverAccountValue.setBalance(receiverAccountValue.getBalance() + oOutput.getAmount());

			boolean isExistToken = false;
			for (int i = 0; i < receiverAccountValue.getTokensCount(); i++) {
				if (receiverAccountValue.getTokens(i).getToken().equals(token)) {
					receiverAccountValue.setTokens(i, receiverAccountValue.getTokens(i).toBuilder()
							.setBalance(receiverAccountValue.getTokens(i).getBalance() + oOutput.getAmount()));
					isExistToken = true;
					break;
				}
			}

			// 如果对应账户中没有该token，则直接创建
			if (!isExistToken) {
				AccountTokenValue.Builder oAccountTokenValue = AccountTokenValue.newBuilder();
				oAccountTokenValue.setToken(token);
				oAccountTokenValue.setBalance(oOutput.getAmount());
				receiverAccountValue.addTokens(oAccountTokenValue);
			}

			keys.add(OEntityBuilder.byteKey2OKey(receiver.getAddress().toByteArray()));
			values.add(OEntityBuilder.byteValue2OValue(receiverAccountValue.build().toByteArray()));
			
		}
		this.keys.addAll(keys);
		this.values.addAll(values);
		// oAccountHelper.BatchPutAccounts(keys, values);
	}
}
