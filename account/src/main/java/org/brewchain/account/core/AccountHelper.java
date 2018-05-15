package org.brewchain.account.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.account.dao.DefDaos;
import org.brewchain.account.util.FastByteComparisons;
import org.brewchain.account.util.OEntityBuilder;
import org.brewchain.bcapi.backend.ODBException;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;
import org.brewchain.account.gens.Act.Account;
import org.brewchain.account.gens.Act.AccountCryptoToken;
import org.brewchain.account.gens.Act.AccountCryptoValue;
import org.brewchain.account.gens.Act.AccountTokenValue;
import org.brewchain.account.gens.Act.AccountValue;
import org.brewchain.account.gens.Act.Contract;
import org.brewchain.account.gens.Act.ContractValue;
import org.brewchain.account.gens.Act.ICO;
import org.brewchain.account.gens.Act.ICOValue;
import org.fc.brewchain.bcapi.EncAPI;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

/**
 * @author sean 用于账户的存储逻辑封装
 * 
 */
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "Account_Helper")
@Slf4j
@Data
public class AccountHelper implements ActorService {
	@ActorRequire(name = "Def_Daos", scope = "global")
	DefDaos dao;

	@ActorRequire(name = "bc_encoder", scope = "global")
	EncAPI encApi;

	public AccountHelper() {
	}

	public synchronized Account CreateAccount(byte[] address, byte[] pubKey) {
		return CreateUnionAccount(address, pubKey, 0, 0, 0, null);
	}

	public synchronized Account CreateUnionAccount(byte[] address, byte[] pubKey, long max, long acceptMax,
			int acceptLimit, List<ByteString> addresses) {
		Account.Builder oUnionAccount = Account.newBuilder();
		AccountValue.Builder oUnionAccountValue = AccountValue.newBuilder();

		oUnionAccountValue.setAcceptLimit(acceptLimit);
		oUnionAccountValue.setAcceptMax(acceptMax);
		if (addresses != null)
			oUnionAccountValue.addAllAddress(addresses);

		oUnionAccountValue.setBalance(KeyConstant.EMPTY_BALANCE.intValue());
		oUnionAccountValue.setMax(max);
		oUnionAccountValue.setNonce(KeyConstant.EMPTY_NONCE.intValue());
		oUnionAccountValue.setPubKey(ByteString.copyFrom(pubKey));

		oUnionAccount.setAddress(ByteString.copyFrom(address));
		oUnionAccount.setValue(oUnionAccountValue);

		return CreateUnionAccount(oUnionAccount.build());
	}

	public synchronized Account CreateUnionAccount(Account oAccount) {
		putAccountValue(oAccount.getAddress().toByteArray(), oAccount.getValue());
		return oAccount;
	}

	/**
	 * 创建合约账户
	 * 
	 * @param address
	 * @param code
	 * @return
	 */
	public synchronized Contract createContractAccount(byte[] address, byte[] code) {
		Contract.Builder oContract = Contract.newBuilder();
		ContractValue.Builder oContractValue = ContractValue.newBuilder();
		oContract.setAddress(ByteString.copyFrom(address));

		oContractValue.setBalance(KeyConstant.EMPTY_BALANCE.intValue());
		oContractValue.setNonce(KeyConstant.EMPTY_NONCE.intValue());
		oContractValue.setCode(ByteString.copyFrom(code));
		oContractValue.setCodeHash(ByteString.copyFrom(encApi.sha3Encode(code)));
		oContract.setValue(oContractValue);
		putContractValue(address, oContractValue.build());
		return oContract.build();
	}

	/**
	 * 移除账户。删除后不可恢复。
	 * 
	 * @param address
	 */
	public synchronized void DeleteAccount(byte[] address) {
		dao.getAccountDao().delete(OEntityBuilder.byteKey2OKey(address));
	}

	/**
	 * 账户是否存在
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public boolean isExist(byte[] addr) throws Exception {
		return GetAccount(addr) != null;
	}

	/**
	 * 获取用户账户
	 * 
	 * @param addr
	 * @return
	 */
	public Account GetAccount(byte[] addr) {
		try {
			OValue oValue = dao.getAccountDao().get(OEntityBuilder.byteKey2OKey(addr)).get();
			AccountValue.Builder oAccountValue = AccountValue.newBuilder();
			oAccountValue.mergeFrom(oValue.getExtdata());

			Account.Builder oAccount = Account.newBuilder();
			oAccount.setAddress(ByteString.copyFrom(addr));
			oAccount.setValue(oAccountValue);
			return oAccount.build();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	/**
	 * 获取合约信息
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public Contract GetContract(byte[] addr) throws Exception {
		OValue oValue = dao.getContractDao().get(OEntityBuilder.byteKey2OKey(addr)).get();
		ContractValue.Builder oContractValue = ContractValue.newBuilder();
		oContractValue.mergeFrom(oValue.getExtdata());

		Contract.Builder oContract = Contract.newBuilder();
		oContract.setAddress(ByteString.copyFrom(addr));
		oContract.setValue(oContractValue);
		return oContract.build();
	}

	/**
	 * Nonce自增1
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public synchronized int IncreaseNonce(byte[] addr) throws Exception {
		return setNonce(addr, 1);
	}

	/**
	 * 增加用户账户余额
	 * 
	 * @param addr
	 * @param balance
	 * @return
	 * @throws Exception
	 */
	public synchronized long addBalance(byte[] addr, long balance) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		oAccountValue.setBalance(oAccountValue.getBalance() + balance);
		putAccountValue(addr, oAccountValue.build());
		return oAccountValue.getBalance();
	}

	/**
	 * 增加用户代币账户余额
	 * 
	 * @param addr
	 * @param balance
	 * @return
	 * @throws Exception
	 */
	public synchronized long addTokenBalance(byte[] addr, String token, long balance) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();

		for (int i = 0; i < oAccountValue.getTokensCount(); i++) {
			if (oAccountValue.getTokens(i).getToken().equals(token)) {
				oAccountValue.setTokens(i, oAccountValue.getTokens(i).toBuilder()
						.setBalance(oAccountValue.getTokens(i).getBalance() + balance));
				putAccountValue(addr, oAccountValue.build());
				return oAccountValue.getTokens(i).getBalance();
			}
		}
		// 如果token账户余额不存在，直接增加一条记录
		AccountTokenValue.Builder oAccountTokenValue = AccountTokenValue.newBuilder();
		oAccountTokenValue.setBalance(balance);
		oAccountTokenValue.setToken(token);
		oAccountValue.addTokens(oAccountTokenValue);
		putAccountValue(addr, oAccountValue.build());
		return oAccountTokenValue.getBalance();
	}

	/**
	 * 增加加密Token账户余额
	 * 
	 * @param addr
	 * @param symbol
	 * @param hash
	 * @return
	 * @throws Exception
	 */
	public synchronized long addCryptoBalance(byte[] addr, String symbol, byte[] hash) throws Exception {
		throw new Exception("未实现该方法");
		// Account.Builder oAccount = GetAccount(addr).toBuilder();
		// AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		//
		// for (int i = 0; i < oAccountValue.getCryptosList().size(); i++) {
		// if (oAccountValue.getCryptosList().get(i).getSymbol().equals(symbol))
		// {
		// oAccountValue.getCryptosList().set(i,
		// oAccountValue.getCryptos(i).toBuilder().addTokens(token).build());
		// putAccountValue(addr, oAccountValue.build());
		// return oAccountValue.getCryptosList().get(i).getTokensCount();
		// }
		// }
		//
		// // 如果是第一个，直接增加一个
		// AccountCryptoValue.Builder oAccountCryptoValue =
		// AccountCryptoValue.newBuilder();
		// oAccountCryptoValue.setSymbol(symbol);
		// oAccountCryptoValue.addTokens(token);
		// oAccountValue.addCryptos(oAccountCryptoValue.build());
		// putAccountValue(addr, oAccountValue.build());
		// return 1;
	}

	/**
	 * 增加加密Token账户余额
	 * 
	 * @param addr
	 * @param symbol
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public synchronized long addCryptoBalance(byte[] addr, String symbol, AccountCryptoToken.Builder token)
			throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();

		for (int i = 0; i < oAccountValue.getCryptosList().size(); i++) {
			if (oAccountValue.getCryptosList().get(i).getSymbol().equals(symbol)) {
				token.setOwner(ByteString.copyFrom(addr));
				token.setNonce(token.getNonce() + 1);
				oAccountValue.getCryptosList().set(i, oAccountValue.getCryptos(i).toBuilder().addTokens(token).build());
				putAccountValue(addr, oAccountValue.build());
				return oAccountValue.getCryptosList().get(i).getTokensCount();
			}
		}

		// 如果是第一个，直接增加一个
		AccountCryptoValue.Builder oAccountCryptoValue = AccountCryptoValue.newBuilder();
		oAccountCryptoValue.setSymbol(symbol);
		oAccountCryptoValue.addTokens(token);
		oAccountValue.addCryptos(oAccountCryptoValue.build());
		putAccountValue(addr, oAccountValue.build());
		return 1;
	}

	/**
	 * 移除加密Token
	 * 
	 * @param addr
	 * @param symbol
	 * @param hash
	 * @return
	 */
	public synchronized long removeCryptoBalance(byte[] addr, String symbol, byte[] hash) {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();

		int retBalance = 0;
		for (int i = 0; i < oAccountValue.getCryptosList().size(); i++) {
			if (oAccountValue.getCryptosList().get(i).getSymbol().equals(symbol)) {
				AccountCryptoValue.Builder value = oAccountValue.getCryptosList().get(i).toBuilder();

				for (int j = 0; j < value.getTokensCount(); j++) {
					if (FastByteComparisons.equal(value.getTokensBuilderList().get(j).getHash().toByteArray(), hash)) {
						value.removeTokens(j);
						break;
					}
				}
				oAccountValue.setCryptos(i, value);
				retBalance = value.getTokensCount();
				break;
			}
		}
		putAccountValue(addr, oAccountValue.build());
		return retBalance;
	}

	/**
	 * 设置用户账户Nonce
	 * 
	 * @param addr
	 * @param nonce
	 * @return
	 * @throws Exception
	 */
	public synchronized int setNonce(byte[] addr, int nonce) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		oAccountValue.setNonce(oAccountValue.getNonce() + nonce);
		putAccountValue(addr, oAccountValue.build());
		return oAccountValue.getNonce();
	}

	/**
	 * 获取用户账户Nonce
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public int getNonce(byte[] addr) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		return oAccountValue.getNonce();
	}

	/**
	 * 获取用户账户的Balance
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public long getBalance(byte[] addr) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		return oAccountValue.getBalance();
	}

	/**
	 * 获取用户Token账户的Balance
	 * 
	 * @param addr
	 * @return
	 * @throws Exception
	 */
	public long getTokenBalance(byte[] addr, String token) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		for (int i = 0; i < oAccountValue.getTokensCount(); i++) {
			if (oAccountValue.getTokens(i).getToken().equals(token)) {
				return oAccountValue.getTokens(i).getBalance();
			}
		}
		return 0;
	}

	/**
	 * 获取加密Token账户的余额
	 * 
	 * @param addr
	 * @param symbol
	 * @return
	 * @throws Exception
	 */
	public List<AccountCryptoToken> getCryptoTokenBalance(byte[] addr, String symbol) throws Exception {
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();

		for (int i = 0; i < oAccountValue.getCryptosCount(); i++) {
			if (oAccountValue.getCryptos(i).getSymbol().equals(symbol)) {
				return oAccountValue.getCryptos(i).getTokensList();
			}
		}

		return new ArrayList<AccountCryptoToken>();
	}

	/**
	 * 生成加密Token方法。 调用时必须确保symbol不重复。
	 * 
	 * @param addr
	 * @param symbol
	 * @param name
	 * @param code
	 * @throws Exception
	 */
	public synchronized void generateCryptoToken(byte[] addr, String symbol, String[] name, String[] code)
			throws Exception {
		if (name.length != code.length || name.length == 0) {
			throw new Exception(String.format("待创建的加密token列表的名称 %s 和编号 %s 无效", name.length, code.length));
		}

		int total = name.length;
		Account.Builder oAccount = GetAccount(addr).toBuilder();
		AccountValue.Builder oAccountValue = oAccount.getValue().toBuilder();
		AccountCryptoValue.Builder oAccountCryptoValue = AccountCryptoValue.newBuilder();
		oAccountCryptoValue.setSymbol(symbol);

		for (int i = 0; i < name.length; i++) {
			AccountCryptoToken.Builder oAccountCryptoToken = AccountCryptoToken.newBuilder();
			oAccountCryptoToken.setName(name[i]);
			oAccountCryptoToken.setCode(code[i]);
			oAccountCryptoToken.setIndex(i);
			oAccountCryptoToken.setTotal(total);
			oAccountCryptoToken.setTimestamp(new Date().getTime());
			oAccountCryptoToken
					.setHash(ByteString.copyFrom(encApi.sha256Encode(oAccountCryptoToken.build().toByteArray())));

			oAccountCryptoToken.setOwner(ByteString.copyFrom(addr));
			oAccountCryptoToken.setNonce(0);
			oAccountCryptoValue.addTokens(oAccountCryptoToken);
		}

		oAccountValue.addCryptos(oAccountCryptoValue);
		putAccountValue(addr, oAccountValue.build());
	}

	public void ICO(byte[] addr, String token) throws Exception {
		OValue oValue = dao.getAccountDao().get(OEntityBuilder.byteKey2OKey(KeyConstant.DB_EXISTS_TOKEN)).get();
		ICO.Builder oICO = ICO.parseFrom(oValue.getExtdata().toByteArray()).toBuilder();
		ICOValue.Builder oICOValue = ICOValue.newBuilder();
		oICOValue.setAddress(ByteString.copyFrom(addr));
		oICOValue.setTimestamp((new Date()).getTime());
		oICOValue.setToken(token);
		oICO.addValue(oICOValue);

		dao.getAccountDao().put(OEntityBuilder.byteKey2OKey(KeyConstant.DB_EXISTS_TOKEN),
				OEntityBuilder.byteValue2OValue(oICO.build().toByteArray()));
	}

	/**
	 * 判断token是否已经发行
	 * 
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public boolean isExistsToken(String token) throws Exception {
		OValue oValue = dao.getAccountDao().get(OEntityBuilder.byteKey2OKey(KeyConstant.DB_EXISTS_TOKEN)).get();
		ICO oICO = ICO.parseFrom(oValue.getExtdata().toByteArray());

		for (ICOValue oICOValue : oICO.getValueList()) {
			if (oICOValue.getToken().equals(token)) {
				return true;
			}
		}
		return false;
	}

	private void putAccountValue(byte[] addr, AccountValue oAccountValue) {
		dao.getAccountDao().put(OEntityBuilder.byteKey2OKey(addr),
				OEntityBuilder.byteValue2OValue(oAccountValue.toByteArray()));
	}

	private void putContractValue(byte[] addr, ContractValue oContractValue) {
		dao.getContractDao().put(OEntityBuilder.byteKey2OKey(addr),
				OEntityBuilder.byteValue2OValue(oContractValue.toByteArray()));
	}

	public void BatchPutAccounts(LinkedList<OKey> keys, LinkedList<OValue> values) {
		OKey[] keysArray = new OKey[keys.size()];
		OValue[] valuesArray = new OValue[values.size()];

		dao.getAccountDao().batchPuts(keys.toArray(keysArray), values.toArray(valuesArray));
	}
}
