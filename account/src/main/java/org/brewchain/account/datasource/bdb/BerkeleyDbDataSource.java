package org.brewchain.account.datasource.bdb;
//package org.brewchain.frontend.datasource.bdb;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.locks.ReadWriteLock;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//import org.brewchain.backend.bc_bdb.provider.OBDBImpl;
//import org.brewchain.bcapi.backend.ODBException;
//import org.brewchain.bcapi.gens.Oentity.OKey;
//import org.brewchain.bcapi.gens.Oentity.OValue;
//import org.brewchain.core.config.SystemProperties;
//import org.brewchain.core.datasource.DbSource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import com.google.protobuf.ByteString;
//
//import onight.tfw.ntrans.api.annotation.ActorRequire;
//import onight.tfw.ojpa.api.DomainDaoSupport;
//import onight.tfw.ojpa.api.StoreServiceProvider;
//
//public class BerkeleyDbDataSource implements DbSource<byte[]> {
//
//	@ActorRequire(name = "bdb_provider", scope = "global")
//	private StoreServiceProvider bdbProvider;
//
//	private String name;
//	private OBDBImpl db;
//	boolean alive;
//	private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();
//	private static final Logger logger = LoggerFactory.getLogger("db");
//
//	@Autowired
//	SystemProperties config = SystemProperties.getDefault();
//
//	public BerkeleyDbDataSource(String name) {
//		this.name = name;
//		logger.debug("New RocksDbDataSource: " + name);
//	}
//
//	@Override
//	public void updateBatch(Map<byte[], byte[]> rows) {
//		List<OKey> keys = new ArrayList<OKey>();
//		List<OValue> values = new ArrayList<OValue>();
//
//		for (byte[] key : rows.keySet()) {
//			OKey.Builder oOKey = OKey.newBuilder();
//			oOKey.setData(ByteString.copyFrom(key));
//			keys.add(oOKey.build());
//
//			OValue.Builder oOValue = OValue.newBuilder();
//			oOValue.setExtdata(ByteString.copyFrom(rows.get(key)));
//			values.add(oOValue.build());
//		}
//
//		this.db.batchPuts((OKey[]) keys.toArray(), (OValue[]) values.toArray());
//	}
//
//	@Override
//	public void put(byte[] key, byte[] val) {
//		OKey.Builder oOKey = OKey.newBuilder();
//		oOKey.setData(ByteString.copyFrom(key));
//
//		OValue.Builder oOValue = OValue.newBuilder();
//		oOValue.setExtdata(ByteString.copyFrom(val));
//		this.db.put(oOKey.build(), oOValue.build());
//	}
//
//	@Override
//	public byte[] get(byte[] key) {
//		OKey.Builder oOKey = OKey.newBuilder();
//		oOKey.setData(ByteString.copyFrom(key));
//		try {
//			return this.db.get(oOKey.build()).get().getExtdata().toByteArray();
//		} catch (ODBException e) {
//			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//
//	@Override
//	public void delete(byte[] key) {
//		OKey.Builder oOKey = OKey.newBuilder();
//		oOKey.setData(ByteString.copyFrom(key));
//		this.db.delete(oOKey.build());
//	}
//
//	@Override
//	public boolean flush() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	@Override
//	public String getName() {
//		return this.name;
//	}
//
//	@Override
//	public void init() {
//		this.db = (OBDBImpl)bdbProvider.getDaoByBeanName(new OBDBImpl(this.name, null));
//	}
//
//	@Override
//	public boolean isAlive() {
//		return alive;
//	}
//
//	@Override
//	public void close() {
//		resetDbLock.writeLock().lock();
//		try {
//			if (!isAlive())
//				return;
//
//			logger.debug("Close db: {}", name);
//			db.close();
//
//			alive = false;
//
//		} catch (Exception e) {
//			logger.error("Error closing db '{}'", name, e);
//		} finally {
//			resetDbLock.writeLock().unlock();
//		}
//	}
//
//	@Override
//	public Set<byte[]> keys() throws RuntimeException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
