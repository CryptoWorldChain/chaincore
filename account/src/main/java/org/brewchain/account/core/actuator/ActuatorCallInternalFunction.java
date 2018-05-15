package org.brewchain.account.core.actuator;

import java.util.Map;

import org.brewchain.account.call.gens.Call.InternalCallArguments;
import org.brewchain.account.core.AccountHelper;
import org.brewchain.account.core.BlockHelper;
import org.brewchain.account.core.TransactionHelper;
import org.brewchain.account.function.InternalFunction;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransaction.Builder;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

public class ActuatorCallInternalFunction extends AbstractTransactionActuator implements iTransactionActuator {

	@Override
	public boolean needSignature() {
		// 执行内部方法调用不需要进行签名
		return false;
	}

	@Override
	public void onExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception {
		InternalCallArguments.Builder oInternalCallArguments = InternalCallArguments
				.parseFrom(oMultiTransaction.getTxBody().getExdata()).toBuilder();

		for (int i = 0; i < InternalFunction.class.getMethods().length; i++) {
			if (InternalFunction.class.getMethods()[i].getName().equals(oInternalCallArguments.getMethod())) {
				if (oInternalCallArguments.getParamsCount() != 0)
					InternalFunction.class.getMethods()[i].invoke(null,
							new Object[] { oAccountHelper, oInternalCallArguments.getParamsList() });
				else
					InternalFunction.class.getMethods()[i].invoke(null, new Object[] { oAccountHelper, new String[] {} });
				break;
			}
		}
		// super.onExecute(oMultiTransaction, senders, receivers);
	}

	@Override
	public void onExecuteDone(MultiTransaction oMultiTransaction) throws Exception {
		// TODO Auto-generated method stub
		super.onExecuteDone(oMultiTransaction);
	}

	public ActuatorCallInternalFunction(AccountHelper oAccountHelper, TransactionHelper oTransactionHelper,
			BlockHelper oBlockHelper, EncAPI encApi) {
		super(oAccountHelper, oTransactionHelper, oBlockHelper, encApi);
		// TODO Auto-generated constructor stub
	}

}
