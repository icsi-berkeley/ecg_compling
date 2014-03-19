// =============================================================================
// File        : HashList.java
// Author      : emok
// Change Log  : Created on Mar 4, 2007
//=============================================================================

package compling.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import compling.util.MapFactory.HashMapFactory;
import compling.util.SetFactory.HashSetFactory;

//=============================================================================

public class MapSet<K, V> implements Map<K, Set<V>> {

	private static final long serialVersionUID = -7822343505816498019L;

	private MapFactory<K, Set<V>> mapFactory;
	private SetFactory<V> setFactory;

	private Map<K, Set<V>> map;

	public MapSet() {
		this(null, null, null);
	}

	public MapSet(Map<K, V> values) {
		this(null, null, values);
	}

	public MapSet(MapFactory<K, Set<V>> mapFactory, SetFactory<V> setFactory) {
		this(mapFactory, setFactory, null);
	}

	public MapSet(MapFactory<K, Set<V>> mapFactory, SetFactory<V> setFactory, Map<K, V> values) {
		this.mapFactory = mapFactory != null ? mapFactory : new HashMapFactory<K, Set<V>>();
		this.setFactory = setFactory != null ? setFactory : new HashSetFactory<V>();

		map = this.mapFactory.buildMap();

		if (values != null) {
			for (K key : values.keySet()) {
				put(key, values.get(key));
			}
		}
	}

	public MapSet(MapSet<K, V> values) {
		this.mapFactory = values.mapFactory;
		this.setFactory = values.setFactory;
		map = this.mapFactory.buildMap();
		if (values != null) {
			for (K key : values.keySet()) {
				putAll(key, values.get(key));
			}
		}
	}

	// /-------------------------------------------------------------------------
	/**
	 * add <code>value</code> to the set of values corresponding to the supplied <code>key</code>. No existing values are
	 * replaced.
	 * 
	 * @param key
	 * @param value
	 * @return the updated number of values associated with the key
	 */
	public int put(K key, V value) {
		if (!this.containsKey(key)) {
			put(key, setFactory.buildSet());
		}
		get(key).add(value);
		return get(key).size();
	}

	// /-------------------------------------------------------------------------
	/**
	 * add all of the <code>values</code> in the collection to the set of values corresponding to the supplied
	 * <code>key</code>. No existing values are replaced.
	 * 
	 * @param key
	 * @param value
	 */

	public void putAll(K key, Collection<V> values) {
		if (!this.containsKey(key)) {
			put(key, setFactory.buildSet());
		}
		get(key).addAll(values);
	}

	public boolean contains(K key, V value) {
		return this.containsKey(key) ? this.get(key).contains(value) : false;
	}

	public void clear() {
		map.clear();
	}

	public boolean containsKey(Object arg0) {
		return map.containsKey(arg0);
	}

	public boolean containsValue(Object arg0) {
		return map.containsValue(arg0);
	}

	public Set<Entry<K, Set<V>>> entrySet() {
		return map.entrySet();
	}

	public Set<V> get(Object arg0) {
		return map.get(arg0);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K> keySet() {
		return map.keySet();
	}

	public Set<V> put(K arg0, Set<V> arg1) {
		return map.put(arg0, arg1);
	}

	public void putAll(Map<? extends K, ? extends Set<V>> arg0) {
		map.putAll(arg0);
	}

	public Set<V> remove(Object arg0) {
		return map.remove(arg0);
	}

	public int size() {
		return map.size();
	}

	public Collection<Set<V>> values() {
		return map.values();
	}

	public Set<V> allValues() {
		Set<V> allValues = new LinkedHashSet<V>();
		for (K key : keySet()) {
			allValues.addAll(get(key));
		}
		return allValues;
	}

	public void initialize(Collection<K> keys) {
		for (K key : keys) {
			if (!this.containsKey(key)) {
				put(key, setFactory.buildSet());
			}
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (K key : keySet()) {
			sb.append(key.toString()).append(": {");
			for (V value : get(key)) {
				sb.append(value.toString()).append(", ");
			}
			sb.deleteCharAt(sb.length() - 1).deleteCharAt(sb.length() - 1);
			sb.append("}").append("\n");
		}
		return sb.toString();
	}
}
