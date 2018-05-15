package org.brewchain.account.core.actuator;

import java.util.Map;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

/**
 * 发布token交易。 发送方token=发布总量
 * 
 * @author
 *
 */
public class ActuatorCreateToken extends AbstractTransactionActuator implements iTransactionActuator {

	public ActuatorCreateToken(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper,
			BlockHelper oBlockHelper, EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
	}

	@Override
	public void onExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		MultiTransactionInput input = oMultiTransaction.getTxBody().getInputs(0);
		oAccountHelper.addTokenBalance(input.getAddress().toByteArray(), input.getToken(), input.getAmount());
		oAccountHelper.ICO(input.getAddress().toByteArray(), input.getToken());
	}

	@Override
	public void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		if (oMultiTransaction.getTxBody().getInputsCount() != 1) {
			throw new Exception(String.format("不允许存在多个发行方"));
		}
		String token = oMultiTransaction.getTxBody().getInputs(0).getToken();
		if (token == null || token.isEmpty()) {
			throw new Exception(String.format("Token交易中Token不允许为空"));
		}

		// 判断nonce是否一致
		Account sender = senders.get(oMultiTransaction.getTxBody().getInputs(0).getAddress());
		AccountValue.Builder senderAccountValue = sender.getValue().toBuilder();

		int nonce = senderAccountValue.getNonce();
		if (nonce != oMultiTransaction.getTxBody().getInputs(0).getNonce()) {
			throw new Exception(String.format("用户的交易索引 %s 与交易的索引不一致 %s", nonce,
					oMultiTransaction.getTxBody().getInputs(0).getNonce()));
		}
		// Token不允许重复
		if (oAccountHelper.isExistsToken(token)) {
			throw new Exception(String.format("不允许重复发行token %s", token));
		}
	}

}
