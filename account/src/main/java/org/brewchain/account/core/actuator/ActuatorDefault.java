package org.brewchain.account.core.actuator;

import java.util.Map;

import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.util.ByteUtil;
import org.fc.brewchain.bcapi.EncAPI;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransactionInput;
import org.brewchain.account.gens.Tx.MultiTransactionOutput;

import com.google.protobuf.ByteString;

public class ActuatorDefault extends AbstractTransactionActuator implements iTransactionActuator {

	public ActuatorDefault(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper, BlockHelper oBlockHelper,
			EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {

		for (MultiTransactionInput oInput : oMultiTransaction.getTxBody().getInputsList()) {
			if (!senders.containsKey(oInput.getAddress())) {
				throw new Exception(String.format("交易的发送方账户 %s 不存在", oInput.getAddress().toString()));
			}
		}

		for (MultiTransactionOutput oOutput : oMultiTransaction.getTxBody().getOutputsList()) {
			if (!receivers.containsKey(oOutput.getAddress())) {
				oAccountHelper.CreateAccount(oOutput.getAddress().toByteArray(), ByteUtil.EMPTY_BYTE_ARRAY);
			}
		}
	}
}
