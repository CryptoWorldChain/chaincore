package org.brewchain.account.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.account.util.ALock;
import org.brewchain.account.util.ByteArrayMap;
import org.brewchain.account.gens.Block.BlockEntity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name="Block_StorageDB")
@Slf4j
@Data
public class BlockStorageDB implements ActorService {
	protected final Map<byte[], byte[]> storage;

	protected ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected ALock readLock = new ALock(rwLock.readLock());
	protected ALock writeLock = new ALock(rwLock.writeLock());
	private BlockEntity lastBlock;
	
	public BlockStorageDB() {
		this(new ByteArrayMap<byte[]>());
	}

	public BlockStorageDB(ByteArrayMap<byte[]> storage) {
		this.storage = storage;
	}

	public void put(byte[] key, byte[] val) {
		if (val == null) {
			delete(key);
		} else {
			try (ALock l = writeLock.lock()) {
				storage.put(key, val);
			}
		}
	}

	public byte[] get(byte[] key) {
		try (ALock l = readLock.lock()) {
			return storage.get(key);
		}
	}

	public void delete(byte[] key) {
		try (ALock l = writeLock.lock()) {
			storage.remove(key);
		}
	}

	public Set<byte[]> keys() {
		try (ALock l = readLock.lock()) {
			return getStorage().keySet();
		}
	}

	public void updateBatch(Map<byte[], byte[]> rows) {
		try (ALock l = writeLock.lock()) {
			for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
		}
	}

	public Map<byte[], byte[]> getStorage() {
		return storage;
	}
}
