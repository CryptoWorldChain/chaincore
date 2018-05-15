package org.brewchain.raftnet;

import org.brewchain.bcapi.backend.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class ODSRaftDao extends ODBDao {

	public ODSRaftDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "raftnet.prop";
	}

	
}
