package compling.util;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains a two-way map between a set of objects and contiguous integers from 0 to the number of objects. Use get(i)
 * to look up object i, and indexOf(object) to look up the index of an object.
 * 
 * @author Dan Klein
 */
public class Indexer<E> extends AbstractList<E> implements Serializable {

	List<E> objects;
	Map<E, Integer> indexes;

	public static final boolean INTERNEDKEYS = true;

	/**
	 * Return the object with the given index
	 * 
	 * @param index
	 */
	public E get(int index) {
		return objects.get(index);
	}

	/**
	 * Return the object with the given index
	 * 
	 * @param index
	 */
	public List<E> getObjects() {
		return objects;
	}

	/**
	 * Returns the number of objects indexed.
	 */
	public int size() {
		return objects.size();
	}

	/**
	 * Returns the index of the given object, or -1 if the object is not present in the indexer.
	 * 
	 * @param o
	 * @return
	 */
	public int indexOf(Object o) {
		Integer index = indexes.get(o);
		if (index == null)
			return -1;
		return index;
	}

	/**
	 * Constant time override for contains.
	 */
	public boolean contains(Object o) {
		return indexes.keySet().contains(o);
	}

	/**
	 * Add an element to the indexer. If the element is already in the indexer, the indexer is unchanged (and false is
	 * returned).
	 * 
	 * @param e
	 * @return
	 */
	public boolean add(E e) {
		if (contains(e))
			return false;
		objects.add(e);
		indexes.put(e, size() - 1);
		return true;
	}

	public Indexer() {
		objects = new ArrayList<E>();
		indexes = new HashMap<E, Integer>();
	}

	public Indexer(boolean internedKeys) {
		objects = new ArrayList<E>();
		if (internedKeys) {
			indexes = new IdentityHashMap<E, Integer>();
		}
		else {
			indexes = new HashMap<E, Integer>();
		}
	}
}
