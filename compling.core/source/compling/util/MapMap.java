// =============================================================================
// File        : MapMap.java
// Author      : emok
// Change Log  : Created on Mar 15, 2007
//=============================================================================

package compling.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import compling.util.MapFactory.HashMapFactory;

//=============================================================================

public class MapMap<K1, K2, V> implements Map<K1, Map<K2, V>> {

	private static final long serialVersionUID = 6832809969566592467L;
	private MapFactory<K1, Map<K2, V>> mapFactory1;
	private MapFactory<K2, V> mapFactory2;
	private Map<K1, Map<K2, V>> map;

	public MapMap() {
		this(null, null, null);
	}

	public MapMap(MapFactory<K1, Map<K2, V>> mapFactory1, MapFactory<K2, V> mapFactory2) {
		this(null, mapFactory1, mapFactory2);
	}

	public MapMap(MapMap<K1, K2, V> m) {
		this(m, null, null);
	}

	public MapMap(MapMap<K1, K2, V> m, MapFactory<K1, Map<K2, V>> mapFactory1, MapFactory<K2, V> mapFactory2) {
		this.mapFactory1 = mapFactory1 != null ? mapFactory1 : new HashMapFactory<K1, Map<K2, V>>();
		this.mapFactory2 = mapFactory2 != null ? mapFactory2 : new HashMapFactory<K2, V>();

		map = this.mapFactory1.buildMap();
		if (m != null) {
			for (K1 k : m.keySet()) {
				put(k, mapFactory2.buildMap());
				get(k).putAll(m.get(k));
			}
		}
	}

	public V put(K1 key1, K2 key2, V value) {
		if (!containsKey(key1)) {
			put(key1, mapFactory2.buildMap());
		}
		return get(key1).put(key2, value);
	}

	public V get(K1 key1, K2 key2) {
		if (containsKey(key1)) {
			return get(key1).get(key2);
		}
		else {
			return null;
		}
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

	public Set<Entry<K1, Map<K2, V>>> entrySet() {
		return map.entrySet();
	}

	public Map<K2, V> get(Object arg0) {
		return map.get(arg0);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<K1> keySet() {
		return map.keySet();
	}

	public Map<K2, V> put(K1 arg0, Map<K2, V> arg1) {
		return map.put(arg0, arg1);
	}

	public void putAll(Map<? extends K1, ? extends Map<K2, V>> arg0) {
		map.putAll(arg0);
	}

	public Map<K2, V> remove(Object arg0) {
		return map.remove(arg0);
	}

	public int size() {
		return map.size();
	}

	public Collection<Map<K2, V>> values() {
		return map.values();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (K1 k1 : keySet()) {
			sb.append(k1.toString()).append(":\n");
			for (K2 k2 : get(k1).keySet()) {
				sb.append("\t").append(k2.toString()).append("\t");
				sb.append(get(k1).get(k2).toString()).append("\n");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

}
