package org.brewchain.bcapi.backend;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.brewchain.bcapi.gens.Oentity.OKey;
import org.brewchain.bcapi.gens.Oentity.OValue;

import com.google.protobuf.ByteString;

import onight.tfw.ojpa.api.ServiceSpec;

public class SimpleMemoryDBSupport extends ODBDao {

	public SimpleMemoryDBSupport(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	protected ConcurrentHashMap<OKey, OValue> storage = new ConcurrentHashMap<>();

	@Override
	public synchronized Future<OValue> put(OKey key, OValue value) throws ODBException {
		return ConcurrentUtils.constantFuture(storage.put(key, value));
	}

	@Override
	public synchronized Future<OValue[]> batchPuts(OKey[] key, OValue[] value) throws ODBException {
		if (key.length != value.length) {
			throw new ODBException("key length not equals value length:" + key.length + "==>" + value.length);
		}
		for (int i = 0; i < key.length; i++) {
			storage.put(key[i], value[i]);
		}
		return ConcurrentUtils.constantFuture(value);
	}

	@Override
	public synchronized Future<OValue> compareAndSwap(OKey key, OValue value, OValue compareValue) throws ODBException {
		if (compareValue == null && value == null || compareValue.equals(storage.get(key))) {
			storage.put(key, value);
		} else {
			return ConcurrentUtils.constantFuture(null);
		}
		return ConcurrentUtils.constantFuture(compareValue);
	}

	@Override
	public synchronized Future<OValue[]> batchCompareAndSwap(OKey[] key, OValue[] value, OValue[] compareValue)
			throws ODBException {
		if (key.length != value.length || value.length != compareValue.length) {
			throw new ODBException("key length not equals value length:" + key.length + "==>" + value.length);
		}
		HashMap<OKey, OValue> map = new HashMap<>();
		for (int i = 0; i < key.length; i++) {
			if (compareValue == null && value == null || compareValue.equals(storage.get(key))) {
			} else {
				return ConcurrentUtils.constantFuture(null);
			}

		}
		storage.putAll(map);

		return ConcurrentUtils.constantFuture(compareValue);
	}

	@Override
	public Future<OValue> compareAndDelete(OKey key, OValue compareValue) throws ODBException {
		if (storage.containsKey(key)) {
			storage.remove(key);
		} else {
			return ConcurrentUtils.constantFuture(null);
		}
		return ConcurrentUtils.constantFuture(compareValue);
	}

	@Override
	public Future<OValue[]> batchCompareAndDelete(OKey[] key, OValue[] compareValue) throws ODBException {
		if (key.length != compareValue.length) {
			throw new ODBException("key length not equals value length:" + key.length + "==>" + compareValue.length);
		}
		for (int i = 0; i < key.length; i++) {
			if (!compareValue[i].equals(storage.get(key))) {
				return ConcurrentUtils.constantFuture(null);
			}
		}
		for (int i = 0; i < key.length; i++) {
			storage.remove(key[i]);
		}
		return ConcurrentUtils.constantFuture(compareValue);
	}

	@Override
	public Future<OValue> delete(OKey key) throws ODBException {
		return ConcurrentUtils.constantFuture(storage.remove(key));
	}

	@Override
	public Future<OValue[]> batchDelete(OKey[] key) throws ODBException {
		OValue[] value = new OValue[key.length];
		for (int i = 0; i < key.length; i++) {
			value[i] = storage.remove(key[i]);
		}
		return ConcurrentUtils.constantFuture(value);
	}

	@Override
	public Future<OValue> get(OKey key) throws ODBException {
		return ConcurrentUtils.constantFuture(storage.get(key));
	}

	@Override
	public Future<OValue[]> list(OKey[] key) throws ODBException {
		OValue[] value = new OValue[key.length];
		for (int i = 0; i < key.length; i++) {
			value[i] = storage.get(key[i]);
		}
		return ConcurrentUtils.constantFuture(value);
	}

	@Override
	public void sync() throws ODBException {
		// TODO Auto-generated method stub

	}

	@Override
	public Future<String> putInfo(String key, String value) throws ODBException {
		storage.put(OKey.newBuilder().setData(ByteString.copyFrom(key.getBytes())).build(),
				OValue.newBuilder().setInfo(value).build());
		return ConcurrentUtils.constantFuture(value);
	}
 
	@Override
	public Future<byte[]> putData(String key, byte[] value) throws ODBException {
		storage.put(OKey.newBuilder().setData(ByteString.copyFrom(key.getBytes())).build(),
				OValue.newBuilder().setExtdata(ByteString.copyFrom(value)).build());
		return ConcurrentUtils.constantFuture(value);
	}

}
