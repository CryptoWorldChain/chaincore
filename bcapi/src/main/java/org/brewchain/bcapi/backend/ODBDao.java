package org.brewchain.bcapi.backend;

import java.util.List;
import java.util.concurrent.Future;

import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OPair;
import org.brewchain.bcapi.gens.Oentity.OValue;

import lombok.Data;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;

@Data
public  class ODBDao implements ODBSupport {

	protected ServiceSpec serviceSpec;

	protected ODBSupport daosupport;

	protected String domainName;

	public ODBDao(ServiceSpec serviceSpec) {
		this.serviceSpec = serviceSpec;
	}

	private Class domainClazz = String.class;
	private Class exampleClazz = Object.class;
	private Class keyClazz = Object.class;

	@Override
	public Future<OValue> put(OKey key, OValue value) throws ODBException {
		return daosupport.put(key, value);
	}
	@Override
	public Future<String> putInfo(String key, String value) throws ODBException {
		return daosupport.putInfo(key, value);
	}
	@Override
	public Future<byte[]> putData(String key, byte[] value) throws ODBException {
		return daosupport.putData(key, value);
	}
	@Override
	public Future<OValue[]> batchPuts(OKey[] key, OValue[] value) throws ODBException {
		return daosupport.batchPuts(key, value);
	}
	@Override
	public Future<OValue> compareAndSwap(OKey key, OValue value, OValue compareValue) throws ODBException {
		return daosupport.compareAndSwap(key, value, compareValue);
	}
	@Override
	public Future<OValue[]> batchCompareAndSwap(OKey[] key, OValue[] value, OValue[] compareValue) throws ODBException {
		return daosupport.batchCompareAndSwap(key, value, compareValue);
	}
	@Override
	public Future<OValue> compareAndDelete(OKey key, OValue compareValue) throws ODBException {
		return daosupport.compareAndDelete(key, compareValue);
	}
	@Override
	public Future<OValue[]> batchCompareAndDelete(OKey[] key, OValue[] compareValue) throws ODBException {
		return daosupport.batchCompareAndDelete(key, compareValue);
	}
	@Override
	public Future<OValue> delete(OKey key) throws ODBException {
		return daosupport.delete(key);
	}
	@Override
	public Future<OValue[]> batchDelete(OKey[] key) throws ODBException {
		return daosupport.batchDelete(key);
	}
	@Override
	public Future<OValue> get(OKey key) throws ODBException {
		return daosupport.get(key);
	}
	@Override
	public Future<OValue[]> list(OKey[] key) throws ODBException {
		return daosupport.list(key);
	}
	@Override
	public void sync() throws ODBException {
		daosupport.sync();
	}
	@Override
	public void setDaosupport(DomainDaoSupport dds) {
		this.daosupport = (ODBSupport)dds;
	}
	@Override
	public Future<OValue> get(String key) throws ODBException {
		return daosupport.get(key);
	}
	@Override
	public Future<OValue> put(String key, OValue value) throws ODBException {
		return daosupport.put(key, value);
	}
	@Override
	public Future<List<OPair>> listBySecondKey(String secondKey) throws ODBException {
		return daosupport.listBySecondKey(secondKey);
	}
	@Override
	public Future<List<OPair>> putBySecondKey(String secondKey, OValue[] values) throws ODBException {
		return daosupport.putBySecondKey(secondKey, values);
	}
	@Override
	public Future<List<OPair>> removeBySecondKey(String secondKey, OKey[] keys) throws ODBException {
		return daosupport.removeBySecondKey(secondKey, keys);
	}
}
