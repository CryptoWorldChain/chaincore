package org.brewchain.account.dao;

import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.backend.bc_bdb.provider.BDBProvider;
import org.osgi.framework.BundleContext;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ojpa.api.StoreServiceProvider;

@iPojoBean
@Provides(specifications = { StoreServiceProvider.class, ActorService.class }, strategy = "SINGLETON")
public class AccountDbProvider extends BDBProvider {
	public AccountDbProvider(BundleContext bundleContext) {
		super(bundleContext);

	}

	@Validate
	public void startup() {
		super.startup();
	}
}
