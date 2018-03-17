package onight.act.persist.mysql.provider;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.ORDBProvider;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

@iPojoBean
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
public class MysqlStoreProvider extends ORDBProvider {

	public MysqlStoreProvider(BundleContext bundleContext) {
		super(bundleContext);
	}

	@Override
	public String[] getContextConfigs() {
		return new String[]{"/SpringContext-daoConfig-act.xml","/SpringContext-daoConfig-act-ext.xml"};
	}
	
	@Validate
	public void startup(){
		super.startup();
		
	}
	
	@Invalidate
	public void shutdown(){
		super.shutdown();
	}
	
	@Override
	public String getProviderid() {
		return "actmysql";
	}

}
