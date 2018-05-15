package org.brewchain.dposblk;

import org.brewchain.bcapi.backend.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class ODSDPoSDao extends ODBDao {

	public ODSDPoSDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "dpos.prop";
	}

	
}
