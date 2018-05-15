package org.brewchain.backend.bc_bdb.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;

@iPojoBean
@Component(immediate = true)
@Instantiate(name = "bdb_provider")
@Provides(specifications = {StoreServiceProvider.class,ActorService.class}, strategy = "SINGLETON")
@Slf4j
@Data
public class BDBProvider implements StoreServiceProvider ,ActorService{

	@ServiceProperty(name = "name")
	String name = "bdb_provider";

	
	BundleContext bundleContext;
	public static final String defaultEnvironmentFolder = "appdb";
	@Setter
	@Getter
	String rootPath = "fbs";
	private HashMap<String, OBDBImpl> dbsByDomains = new HashMap<>();
	private Environment dbEnv;

	public BDBProvider(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public String[] getContextConfigs() {
		return new String[] {};
	}

	PropHelper params;

	OBDBImpl default_dbImpl;

	@Validate
	public void startup() {
		try {
			params = new PropHelper(bundleContext);
			String dir = params.get("org.bc.obdb.dir", "odb." + Math.abs(NodeHelper.getCurrNodeListenOutPort() - 5100));
			this.dbEnv = initDatabaseEnvironment(dir);
			DatabaseConfig dbconf = new DatabaseConfig();

			dbconf.setAllowCreate(true);
			dbconf.setSortedDuplicates(false);
			this.dbs = openDatabase("bc_bdb", true, false)[0];
			default_dbImpl = new OBDBImpl("_", dbs);
			dbsByDomains.put("_", default_dbImpl);
			VersionChecker.check(default_dbImpl);
		} catch (Throwable t) {
			log.error("init bc bdb failed", t);
		}
	}

	private Database dbs;

	private Environment initDatabaseEnvironment(String folder) {
		File homeDir = new File(folder);
		if (!homeDir.exists()) {
			if (!homeDir.mkdir()) {
				throw new PersistentMapException("");
			}
		}
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setDurability(Durability.COMMIT_SYNC);
		envConfig.setAllowCreate(true);
		envConfig.setTransactional(true);
		return new Environment(homeDir, envConfig);
	}

	private Database[] openDatabase(String dbNameP, boolean allowCreate, boolean allowDuplicates) {
		DatabaseConfig objDbConf = new DatabaseConfig();
		objDbConf.setAllowCreate(allowCreate);
		objDbConf.setSortedDuplicates(allowDuplicates);
		objDbConf.setDeferredWrite(false);
		objDbConf.setTransactional(true);
		
		String dbsname[] = dbNameP.split("\\.");
		Database db = this.dbEnv.openDatabase(null, dbsname[0], objDbConf);

		if (dbsname.length == 2) {
			SecondaryConfig sd = new SecondaryConfig();
			sd.setAllowCreate(allowCreate);
			sd.setAllowPopulate(true);
			sd.setSortedDuplicates(true);
			sd.setDeferredWrite(false);
			sd.setTransactional(true);
			ODBTupleBinding tb = new ODBTupleBinding();
			SecondaryKeyCreator keyCreator = new ODBSecondKeyCreator(tb);
			sd.setKeyCreator(keyCreator);
			
			SecondaryDatabase sdb = this.dbEnv.openSecondaryDatabase(null, dbNameP, db, sd);
			return new Database[] { db, sdb };
		} else {
			return new Database[] { db };
		}
	}

	@Invalidate
	public void shutdown() {
		Iterator<String> it = this.dbsByDomains.keySet().iterator();
		while (it.hasNext()) {
			try {
				this.dbsByDomains.get(it.next()).close();
			} catch (DatabaseException e) {
				log.warn("close db error", e);
			}
		}

		this.dbEnv.close();

	}

	@Override
	public String getProviderid() {
		return "bc_bdb";
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dds) {
		OBDBImpl dbi = dbsByDomains.get(dds.getDomainName());
		if (dbi == null) {
			synchronized (dbsByDomains) {
				dbi = dbsByDomains.get(dds.getDomainName());
				if (dbi == null) {
					Database[] dbs = openDatabase("bc_bdb_" + dds.getDomainName(), true, false);
					if (dbs.length == 1) {
						dbi = new OBDBImpl(dds.getDomainName(), dbs[0]);
					} else {
						dbi = new OBDBImpl(dds.getDomainName(), dbs[0], dbs[1]);
					}
					dbsByDomains.put(dds.getDomainName(), dbi);
				}
			}
		}
		return dbi;
	}

}
