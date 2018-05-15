package org.brewchain.account.util;

import java.util.*;
import java.util.Map.Entry;

public class ByteArrayMap<V> implements Map<byte[], byte[]> {
	private final LinkedHashMap<byte[], byte[]> delegate;

	public ByteArrayMap() {
		this(new LinkedHashMap<byte[], byte[]>());
	}

	public ByteArrayMap(LinkedHashMap<byte[], byte[]> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey((byte[]) key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public byte[] get(Object key) {
		return delegate.get((byte[]) key);
	}

	@Override
	public byte[] put(byte[] key, byte[] value) {
		return delegate.put(key, value);
	}

	@Override
	public byte[] remove(Object key) {
		return delegate.remove((byte[]) key);
	}

	@Override
	public void putAll(Map<? extends byte[], ? extends byte[]> m) {
		for (Entry<? extends byte[], ? extends byte[]> entry : m.entrySet()) {
			delegate.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public Set<byte[]> keySet() {
		return delegate.keySet();
	}

	@Override
	public Collection<byte[]> values() {
		return delegate.values();
	}

	public Set<Entry<byte[], byte[]>> entrySet() {
		return delegate.entrySet();
	}

	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	private class MapEntrySet implements Set<Map.Entry<byte[], byte[]>> {
		private final Set<Map.Entry<byte[], byte[]>> delegate;

		private MapEntrySet(Set<Entry<byte[], byte[]>> delegate) {
			this.delegate = delegate;
		}

		public int size() {
			return delegate.size();
		}

		public boolean isEmpty() {
			return delegate.isEmpty();
		}

		public boolean contains(Object o) {
			throw new RuntimeException("Not implemented");
		}

		public Iterator<Entry<byte[], byte[]>> iterator() {
			final Iterator<Entry<byte[], byte[]>> it = delegate.iterator();
			return new Iterator<Entry<byte[], byte[]>>() {

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Entry<byte[], byte[]> next() {
					Entry<byte[], byte[]> next = it.next();
					return new AbstractMap.SimpleImmutableEntry(next.getKey(), next.getValue());
				}

				@Override
				public void remove() {
					it.remove();
				}
			};
		}

		@Override
		public Object[] toArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T> T[] toArray(T[] a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean add(java.util.Map.Entry<byte[], byte[]> e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean remove(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean addAll(Collection<? extends java.util.Map.Entry<byte[], byte[]>> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void clear() {
			// TODO Auto-generated method stub

		}
	}

	@Override
	public int size() {
		return delegate.size();
	}
}
