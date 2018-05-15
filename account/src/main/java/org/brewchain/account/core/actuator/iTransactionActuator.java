package org.brewchain.account.core.actuator;

import java.util.LinkedList;
import java.util.Map;

import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Tx.MultiTransaction;
import org.brewchain.account.gens.Tx.MultiTransaction.Builder;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.google.protobuf.ByteString;

public interface iTransactionActuator {
	boolean needSignature();
	LinkedList<OKey> getKeys();
	LinkedList<OValue> getValues();
	/**
	 * 交易签名校验
	 * 
	 * @param oMultiTransaction
	 * @param senders
	 * @param receivers
	 * @throws Exception
	 */
	void onVerifySignature(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception;

	/**
	 * 交易执行前的数据校验。
	 * 
	 * @param oMultiTransaction
	 * @param senders
	 * @param receivers
	 * @throws Exception
	 */
	void onPrepareExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception;

	/**
	 * 交易执行。
	 * 
	 * @param oMultiTransaction
	 * @param senders
	 * @param receivers
	 * @throws Exception
	 */
	void onExecute(MultiTransaction oMultiTransaction, Map<ByteString, Account> senders,
			Map<ByteString, Account> receivers) throws Exception;

	/**
	 * 交易执行成功后。
	 * 
	 * @param oMultiTransaction
	 * @throws Exception
	 */
	void onExecuteDone(MultiTransaction oMultiTransaction) throws Exception;
}
