package org.brewchain.raftnet;

import org.brewchain.bcapi.backend.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class ODSLogIdxDao extends ODBDao {

	public ODSLogIdxDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "raftidxlog";
	}

	
}
