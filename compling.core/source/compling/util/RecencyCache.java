// =============================================================================
// File        : RecencyCache.java
// Author      : John Bryant (refactored by Eva Mok)
// Change Log  : Created on Apr 11, 2007
//=============================================================================

package compling.util;

import java.util.Collection;
import java.util.LinkedList;

//=============================================================================

public class RecencyCache<T> {

	private int maxSize;
	private LinkedList<T> list;

	public RecencyCache(int maxSize) {
		this.maxSize = maxSize;
		list = new LinkedList<T>();
	}

	public int maxSize() {
		return maxSize;
	}

	public void add(T v) {
		// the tricky part about this is the side effect in the remove
		if (!list.remove(v) && list.size() >= maxSize) {
			list.removeLast();
		}
		list.addFirst(v);
	}

	public void addAll(Collection<T> c) {
		for (T v : c) {
			this.add(v);
		}
	}

	public Collection<T> entries() {
		return list;
	}

	public void clear() {
		list.clear();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (T v : this.entries()) {
			sb.append(v.toString()).append(", ");
		}
		return sb.toString();
	}

}