package org.brewchain.account.core.actuator;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.account.gens.Act.AccountCryptoValue;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransaction.Builder;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;
import org.brewchain.account.util.FastByteComparisons;
import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

public class ActuatorCryptoTokenTransaction extends AbstractTransactionActuator implements iTransactionActuator {

	public ActuatorCryptoTokenTransaction(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper,
			BlockHelper oBlockHelper, EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 不校验发送方和接收方的balance的一致性
	 * 
	 * @param oMultiTransaction
	 * @param senders
	 * @param receivers
	 * @throws Exception
	 */
	@Override
	public void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {

		String inputSymbol = "";

		// 发送方账户中必须存在该token
		for (int i = 0; i < oMultiTransaction.getTxBody().getInputsCount(); i++) {
			boolean isTokenExists = false;
			MultiTransactionInput oInput = oMultiTransaction.getTxBody().getInputs(i);
			if (inputSymbol.equals("") && !oInput.getSymbol().isEmpty()) {
				inputSymbol = oInput.getSymbol();
			}
			// if (inputSymbol.equals(oInput.getSymbol()))
			ByteString address = oInput.getAddress();
			Account oAccount = senders.get(address);
			AccountValue oAccountValue = oAccount.getValue();

			for (int j = 0; j < oAccountValue.getCryptosCount(); j++) {
				if (oAccountValue.getCryptos(i).getSymbol().equals(inputSymbol)) {
					AccountCryptoValue oAccountCryptoValue = oAccountValue.getCryptos(i);
					for (int k = 0; k < oAccountCryptoValue.getTokensCount(); k++) {
						if (oAccountCryptoValue.getTokens(k).getHash().equals(oInput.getCryptoToken())) {
							isTokenExists = true;
							break;
						}
					}
				}
				if (isTokenExists) {
					break;
				}
			}
			if (!isTokenExists) {
				throw new Exception(String.format("发送方 %s 的账户中，不存在标记为 %s 的hash为 %s 的加密token，该交易无效", address.toString(),
						inputSymbol, oInput.getCryptoToken().toString()));
			}
		}

		for (int i = 0; i < oMultiTransaction.getTxBody().getOutputsCount(); i++) {
			MultiTransactionOutput oOutput = oMultiTransaction.getTxBody().getOutputs(i);
			if (!oOutput.getSymbol().isEmpty() && !oOutput.getSymbol().equals(inputSymbol)) {
				throw new Exception(
						String.format("发送方的加密token标记 %s 与接收方的加密token标记 %s 不一致", inputSymbol, oOutput.getSymbol()));
			}
		}
	}

	/*
	 * 支持 加密token -> cwb ; cwb -> 加密token ; 加密token -> 加密token;
	 * 
	 * @see
	 * org.brewchain.account.core.actuator.AbstractTransactionActuator#onExecute
	 * (org.brewchain.account.gens.Tx.MultiTransaction.Builder, java.util.Map,
	 * java.util.Map)
	 */
	@Override
	public void onExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		LinkedList<OKey> keys = new LinkedList<OKey>();
		LinkedList<OValue> values = new LinkedList<OValue>();

		Map<byte[], AccountCryptoToken> tokens = new HashMap<byte[], AccountCryptoToken>();
		// 发送方移除balance
		for (int i = 0; i < oMultiTransaction.getTxBody().getInputsCount(); i++) {
			MultiTransactionInput oInput = oMultiTransaction.getTxBody().getInputs(i);

			// tokens.put(oMultiTransaction.get, value);
			Account sender = senders.get(oInput.getAddress());
			AccountValue.Builder oAccountValue = sender.getValue().toBuilder();

			// 不论任何交易类型，都默认执行账户余额的更改
			oAccountValue.setBalance(oAccountValue.getBalance() - oInput.getAmount() - oInput.getFee());

			for (int k = 0; k < oAccountValue.getCryptosCount(); k++) {

				if (oAccountValue.getCryptosList().get(k).getSymbol().equals(oInput.getSymbol())) {
					AccountCryptoValue.Builder value = oAccountValue.getCryptosList().get(i).toBuilder();

					for (int j = 0; j < value.getTokensCount(); j++) {
						if (value.getTokensBuilderList().get(j).getHash().equals(oInput.getCryptoToken())) {
							tokens.put(value.getTokensBuilderList().get(j).getHash().toByteArray(),
									value.getTokensBuilderList().get(j).build());
							value.removeTokens(j);
							break;
						}
					}
					oAccountValue.setCryptos(i, value);
					break;
				}
			}
			oAccountValue.setNonce(oAccountValue.getNonce() + 1);
			keys.add(OEntityBuilder.byteKey2OKey(oInput.getAddress().toByteArray()));
			values.add(OEntityBuilder.byteValue2OValue(oAccountValue.build().toByteArray()));
		}

		// 接收方增加balance
		for (int i = 0; i < oMultiTransaction.getTxBody().getOutputsCount(); i++) {
			MultiTransactionOutput oOutput = oMultiTransaction.getTxBody().getOutputs(i);

			Account receiver = receivers.get(oOutput.getAddress());
			AccountValue.Builder receiverAccountValue = receiver.getValue().toBuilder();

			receiverAccountValue.setBalance(receiverAccountValue.getBalance() + oOutput.getAmount());

			for (int k = 0; k < receiverAccountValue.getCryptosCount(); k++) {
				if (receiverAccountValue.getCryptosList().get(k).getSymbol().equals(oOutput.getSymbol())) {
					AccountCryptoValue.Builder oAccountCryptoValue = receiverAccountValue.getCryptosList().get(k)
							.toBuilder();

					AccountCryptoToken.Builder oAccountCryptoToken = tokens.get(oOutput.getCryptoToken().toByteArray())
							.toBuilder();
					oAccountCryptoToken.setOwner(oOutput.getAddress());
					oAccountCryptoToken.setNonce(oAccountCryptoToken.getNonce() + 1);
					oAccountCryptoToken.setOwnertime((new Date()).getTime());
					oAccountCryptoValue.addTokens(oAccountCryptoToken.build());
					receiverAccountValue.setCryptos(k, oAccountCryptoValue);
				}
			}

			keys.add(OEntityBuilder.byteKey2OKey(oOutput.getAddress().toByteArray()));
			values.add(OEntityBuilder.byteValue2OValue(receiverAccountValue.build().toByteArray()));
		}

		this.keys.addAll(keys);
		this.values.addAll(values);
		// oAccountHelper.BatchPutAccounts(keys, values);
	}

	@Override
	public void onExecuteDone(MultiTransaction oMultiTransaction) throws Exception {
		// TODO Auto-generated method stub
		super.onExecuteDone(oMultiTransaction);
	}

}
