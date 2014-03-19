package compling.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A map from objects to doubles. Includes convenience methods for getting, setting, and incrementing element counts.
 * Objects not in the counter will return a count of zero. The counter is backed by a HashMap (unless specified
 * otherwise with the MapFactory constructor).
 * 
 * @author Dan Klein
 * 
 */
public class Counter<E> implements Serializable, Cloneable {

	private static final long serialVersionUID = -5121264467135061764L;

	/**
	 * Counter.java
	 */
	Map<E, Double> entries;// = new HashMap<E, Double>();
	MapFactory<E, Double> myFactory;
	double total = 0;

	/**
	 * The elements in the counter.
	 * 
	 * @return set of keys
	 */
	public Set<E> keySet() {
		return entries.keySet();
	}

	/**
	 * The number of entries in the counter (not the total count -- use totalCount() instead).
	 */
	public int size() {
		return entries.size();
	}

	/**
	 * True if there are no entries in the counter (false does not mean totalCount > 0)
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Returns whether the counter contains the given key. Note that this is the way to distinguish keys which are in the
	 * counter with count zero, and those which are not in the counter (and will therefore return count zero from
	 * getCount().
	 * 
	 * @param key
	 * @return whether the counter contains the key
	 */
	public boolean containsKey(E key) {
		return entries.containsKey(key);
	}

	public void removeKey(E key) {
		entries.remove(key);
	}

	/**
	 * Get the count of the element, or zero if the element is not in the counter.
	 * 
	 * @param key
	 * @return
	 */
	public double getCount(E key) {
		Double value = entries.get(key);
		if (value == null)
			return 0;
		return value;
	}

	/**
	 * Set the count for the given key, clobbering any previous count.
	 * 
	 * @param key
	 * @param count
	 */
	public void setCount(E key, double count) {
		double oldCount = getCount(key);
		total = total + count - oldCount;
		entries.put(key, count);
	}

	/**
	 * Increment a key's count by the given amount.
	 * 
	 * @param key
	 * @param increment
	 */
	public void incrementCount(E key, double increment) {
		setCount(key, getCount(key) + increment);
	}

	/**
	 * Finds the total of all counts in the counter. This implementation iterates through the entire counter every time
	 * this method is called.
	 * 
	 * @return the counter's total
	 */
	public double totalCount() {
		/*
		 * double total = 0.0; for (Map.Entry<E, Double> entry : entries.entrySet()) { total += entry.getValue(); }
		 */
		return this.total;
	}

	/**
	 * Finds the key with maximum count. This is a linear operation, and ties are broken arbitrarily.
	 * 
	 * @return a key with minumum count
	 */
	public E argMax() {
		double maxCount = Double.NEGATIVE_INFINITY;
		E maxKey = null;
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			if (entry.getValue() > maxCount || maxKey == null) {
				maxKey = entry.getKey();
				maxCount = entry.getValue();
			}
		}
		return maxKey;
	}

	/**
	 * Returns a string representation with the keys ordered by decreasing counts.
	 * 
	 * @return string representation
	 */
	public String toString() {
		return toString(keySet().size());
	}

	/**
	 * Returns a string representation which includes no more than the maxKeysToPrint elements with largest counts.
	 * 
	 * @param maxKeysToPrint
	 * @return partial string representation
	 */
	public String toString(int maxKeysToPrint) {
		return asPriorityQueue().toString(maxKeysToPrint);
	}

	/**
	 * Builds a priority queue whose elements are the counter's elements, and whose priorities are those elements' counts
	 * in the counter.
	 */
	public PriorityQueue<E> asPriorityQueue() {
		PriorityQueue<E> pq = new PriorityQueue<E>(entries.size());
		for (Map.Entry<E, Double> entry : entries.entrySet()) {
			pq.add(entry.getKey(), entry.getValue());
		}
		return pq;
	}

	public void removeLowCountKeys(int minCount) {
		for (Iterator<E> i = keySet().iterator(); i.hasNext();) {
			E key = i.next();
			if (getCount(key) < minCount) {
				i.remove();
				entries.remove(key);
			}
		}
	}

	public Set<E> getKeysWithCount(int count) {
		Set<E> keys = new HashSet<E>();
		for (E key : keySet()) {
			if (getCount(key) == count) {
				keys.add(key);
			}
		}
		return keys;
	}

	public Counter<E> clone() {
		try {
			Counter<E> copy = new Counter<E>(myFactory);
			for (Map.Entry<E, Double> entry : entries.entrySet()) {
				copy.incrementCount(entry.getKey(), entry.getValue());
			}
			return copy;
		}
		catch (Exception e) {
			System.out.println("Inexplicable meltdown in Counter.clone: " + e);
			throw new RuntimeException("Inexplicable meltdown in FeatureStructureSet.clone: " + e);
		}
	}

	private Counter(Map<E, Double> myMap) {
		this.entries = myMap;
	}

	public Counter() {
		this(new MapFactory.HashMapFactory<E, Double>());
	}

	public Counter(MapFactory<E, Double> mf) {
		entries = mf.buildMap();
		myFactory = mf;
	}

	public static void main(String[] args) {
		Counter<String> counter = new Counter<String>();
		System.out.println(counter);
		counter.incrementCount("planets", 7);
		System.out.println(counter);
		counter.incrementCount("planets", 1);
		System.out.println(counter);
		counter.setCount("suns", 1);
		System.out.println(counter);
		counter.setCount("aliens", 0);
		System.out.println(counter);
		System.out.println(counter.toString(2));
		System.out.println("Total: " + counter.totalCount());
	}

}
