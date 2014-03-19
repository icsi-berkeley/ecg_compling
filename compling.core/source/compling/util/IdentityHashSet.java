package compling.util;

import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This class implements set using reference equality. Don't use this class if you don't know what that means.
 * 
 * Additionally, this set doesn't support null elements
 */

public class IdentityHashSet<T> extends AbstractSet<T> implements Cloneable {
	private IdentityHashMap<T, T> store;

	public IdentityHashSet() {
		store = new IdentityHashMap<T, T>();
	}

	public IdentityHashSet(Set<T> copyFrom) {
		this();
		this.addAll(copyFrom);
	}

	public boolean add(T e) {
		if (e == null) {
			throw new IllegalArgumentException();
		}
		if (store.containsKey(e) == true) {
			return false;
		}
		store.put(e, e);
		return true;
	}

	public boolean contains(Object e) {
		return store.containsKey(e);
	}

	public boolean isEmpty() {
		return store.size() == 0;
	}

	public boolean remove(Object e) {
		return store.remove(e) != null;
	}

	public int size() {
		return store.size();
	}

	public Iterator<T> iterator() {
		return store.keySet().iterator();
	}

	public int hashCode() {
		int i = 0;
		for (T e : store.keySet()) {
			i = i + System.identityHashCode(e);
		}
		return i;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IdentityHashSet)) {
			return false;
		}
		Set<T> that = (Set<T>) o;
		if (this.size() != that.size()) {
			return false;
		}
		for (T thatE : store.keySet()) {
			if (!store.containsKey(thatE)) {
				return false;
			}
		}
		return true;
	}

	public Object clone() {
		try {
			IdentityHashSet<T> copy = (IdentityHashSet<T>) super.clone();
			copy.store = (IdentityHashMap<T, T>) store.clone();
			return copy;
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException("The plane crashed into the mountain.\n" + e.toString());
		}
	}

	public static void main(String[] args) {
		IdentityHashSet<String> ihs = new IdentityHashSet<String>();
		ihs.add("hi");
		ihs.add("hi");
		ihs.add("bye");
		String hi = new String("hi");
		ihs.add(hi);
		String bye = new String("bye");
		ihs.add(bye);
		ihs.add(bye);
		for (String s : ihs) {
			System.out.println(s);
		}
		IdentityHashSet<String> ihsTwo = new IdentityHashSet<String>(ihs);
		// ihsTwo.addAll(ihs);
		for (String s : ihsTwo) {
			System.out.println(s);
		}
	}

}
