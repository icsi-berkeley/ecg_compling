package compling.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dan Klein
 */
public final class CollectionUtils {

//	public static <T> String join(Tuple<T> tuple) {
//		return join(tuple, " ");
//	}

//	public static <T> String join(Tuple<T> tuple, String s) {
//		StringBuilder sb = new StringBuilder();
//		if (tuple.size() > 0 ) {
//			int i;
//			for (i = 0; i < tuple.size() - 1; ++i)
//				sb.append(tuple.item(i) + s);
//			
//			sb.append(tuple.item(i));
//		}
//		return sb.toString();
//	}

	public static <K, V> void addToValueList(Map<K, List<V>> map, K key, V value) {
		List<V> valueList = map.get(key);
		if (valueList == null) {
			valueList = new ArrayList<V>();
			map.put(key, valueList);
		}
		valueList.add(value);
	}

	public static <K, V> void addToValueSet(Map<K, Set<V>> map, K key, V value) {
		Set<V> values = map.get(key);
		if (values == null) {
			values = new HashSet<V>();
			map.put(key, values);
		}
		values.add(value);
	}

	public static <K, V> List<V> getValueList(Map<K, List<V>> map, K key) {
		List<V> valueList = map.get(key);
		if (valueList == null)
			return Collections.emptyList();

		return valueList;
	}

	public static <K, V> Set<V> getValueSet(Map<K, Set<V>> map, K key) {
		Set<V> values = map.get(key);
		if (values == null)
			return Collections.emptySet();

		return values;
	}

	public static <E> List<E> iteratorToList(Iterator<E> iterator) {
		List<E> list = new ArrayList<E>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}

	/**
	 * Joins list using the separator s.
	 * 
	 * @param list
	 *           the list to join
	 * @param s
	 *           the separator
	 * @return a new conjoined String
	 */
	public static <T> String join(List<T> list, String s) {
		StringBuilder sb = new StringBuilder();
		if (list.size() > 0) {
			int i;
			for (i = 0; i < list.size() - 1; ++i)
				sb.append(list.get(i) + s);

			sb.append(list.get(i));
		}
		return sb.toString();
	}

	/**
	 * Convenience method for constructing lists on one line. It does type inference:
	 * 
	 * <pre>
	 * List&lt;String&gt; args = makeList(&quot;-length&quot;, &quot;20&quot;, &quot;-parser&quot;, &quot;cky&quot;);
	 * </pre>
	 * 
	 * @param elements
	 * 
	 * @return a new List containing the arguments.
	 */
	public static <T> List<T> makeList(T... elements) {
		List<T> list = new ArrayList<T>();
		for (T elem : elements) {
			list.add(elem);
		}
		return list;
	}

	public static String mul(String s, int times) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < times; ++i)
			sb.append(s);

		return sb.toString();
	}

	public static <E extends Comparable<E>> List<E> sort(Collection<E> c) {
		List<E> list = new ArrayList<E>(c);
		Collections.sort(list);
		return list;
	}

	public static <E> List<E> sort(Collection<E> c, Comparator<E> r) {
		List<E> list = new ArrayList<E>(c);
		Collections.sort(list, r);
		return list;
	}

	public static <T> int sum(Collection<T>[] elements) {
		int a = 0;
		for (Collection<T> e : elements)
			a += e.size();

		return a;
	}

	public static <E> Set<E> union(Set<? extends E> x, Set<? extends E> y) {
		Set<E> union = new HashSet<E>();
		union.addAll(x);
		union.addAll(y);
		return union;
	}

}
