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
public class RocksDBStoreProvider extends ORDBProvider {


	RocksDBSyncWriteJournal  provider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ObjectMapper objectMapper;


	public RocksDBStoreProvider(BundleContext bundleContext) {
		super(bundleContext);
	}

	@Override
	public String[] getContextConfigs() {
		return new String[]{};
	}
	
	AkkaActors actors ;
	@Validate
	public void startup(){
		super.startup();
		provider = new RocksDBSyncWriteJournal();
		
	}

	public StaticTableDaoSupport getStaticDao(String beanname) {
		return actors.getPersistByID(provider,beanname);
	}
	
	@Invalidate
	public void shutdown(){
		super.shutdown();
	}
	
	@Override
	public String getProviderid() {
		return "rocksdb";
	}

}
