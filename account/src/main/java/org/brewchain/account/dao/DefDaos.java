package org.brewchain.account.dao;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.account.gens.Actimpl.PACTModule;
import org.brewchain.bcapi.backend.ODBSupport;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.p22p.core.PZPCtrl;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@NActorProvider
@Data
@Slf4j
@Instantiate(name = "Def_Daos")
public class DefDaos extends SessionModules<Message> {
	@StoreDAO(target = "bc_bdb", daoClass = AccoutDomain.class)
	ODBSupport accountDao;

	@StoreDAO(target = "bc_bdb", daoClass = ContractDomain.class)
	ODBSupport contractDao;

	// @StoreDAO(target = "bc_bdb", daoClass = TxDomain.class)
	// ODBSupport txDao;

	@StoreDAO(target = "bc_bdb", daoClass = TxSecondaryDomain.class)
	ODBSupport txsDao;

	@StoreDAO(target = "bc_bdb", daoClass = BlockDomain.class)
	ODBSupport blockDao;

	@StoreDAO(target = "bc_bdb", daoClass = TxBlockDomain.class)
	ODBSupport txblockDao;

	// @ActorRequire(scope = "global", name = "pzpctrl")
	// PZPCtrl pzp;

	@Override
	public void onDaoServiceAllReady() {
		// log.debug("EncAPI==" + enc);
		// 校验
		log.debug("service ready!!!!");
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport arg0) {
	}

	public void setAccountDao(DomainDaoSupport accountDao) {
		this.accountDao = (ODBSupport) accountDao;
	}

	public ODBSupport getAccountDao() {
		return accountDao;
	}

	public void setContractDao(DomainDaoSupport contractDao) {
		this.contractDao = (ODBSupport) contractDao;
	}

	public ODBSupport getContractDao() {
		return contractDao;
	}

	// public void setTxDao(DomainDaoSupport txDao) {
	// this.txDao = (ODBSupport) txDao;
	// }

	// public ODBSupport getTxDao() {
	// return txDao;
	// }

	public void setBlockDao(DomainDaoSupport blockDao) {
		this.blockDao = (ODBSupport) blockDao;
	}

	public ODBSupport getBlockDao() {
		return blockDao;
	}

	public void setTxsDao(DomainDaoSupport txsDao) {
		this.txsDao = (ODBSupport) txsDao;
	}

	public ODBSupport getTxsDao() {
		return txsDao;
	}

	public void setTxblockDao(DomainDaoSupport txblockDao) {
		this.txblockDao = (ODBSupport) txblockDao;
	}

	public ODBSupport getTxblockDao() {
		return txblockDao;
	}

	@Override
	public String[] getCmds() {
		return new String[] { "DEFDAOS" };
	}

	@Override
	public String getModule() {
		return PACTModule.ACT.name();
	}

	public boolean isReady() {
		if (blockDao != null 
				&& BlockDomain.class.isInstance(blockDao)
				&& blockDao.getDaosupport() != null
				&& txblockDao != null 
				&& TxBlockDomain.class.isInstance(txblockDao)
				&& txblockDao.getDaosupport() != null
				&& txsDao != null 
				&& TxSecondaryDomain.class.isInstance(txsDao)
				&& txsDao.getDaosupport() != null
				&& contractDao != null 
				&& ContractDomain.class.isInstance(contractDao)
				&& contractDao.getDaosupport() != null
				&& accountDao != null
				&& AccoutDomain.class.isInstance(accountDao)
				&& accountDao.getDaosupport() != null) {
			;
			return true;
		}
		return false;
	}

}
