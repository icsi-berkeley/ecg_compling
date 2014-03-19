package compling.parser.ecgparser.morph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

interface IHashComparator<T> {
	boolean equals(T a, T b);

	int hashCode(T a);

	/**
	 * 
	 * @param collectionA
	 * @param collectionB
	 * @param comparator
	 *           Class defining an equals() method to be used for comparison and a hashCode() method for generating a
	 *           unique hash
	 * @param onlyInA
	 *           Will be populated with the elements that appeared only in the first collection
	 * @param onlyInB
	 *           Will be populated with the elements that appeared only in the second collection
	 * @param inBoth
	 *           Will be populated with the elements that appeared in both collections
	 * @return true iff the two collections are equivalent
	 */
	boolean compare(Collection<T> collectionA, Collection<T> collectionB, Set<T> onlyInA, Set<T> onlyInB, Set<T> inBoth)
			throws HashComparator.HashComparisonException;
}

public abstract class HashComparator<T> implements IHashComparator<T> {
	public static class HashComparisonException extends compling.parser.ParserException {
		Object elt;
		int hashCode;

		public HashComparisonException(Object elt, int hashCode) {
			this.elt = elt;
			this.hashCode = hashCode;
		}

		public String getMessage() {
			return "Hash conflict between two unequal elements of set B: " + elt + " with hashCode=" + hashCode;
		}

		public Object getElt() {
			return elt;
		}

		public int getHashCode() {
			return hashCode;
		}
	}

	public boolean compare(Collection<T> collectionA, Collection<T> collectionB, Set<T> onlyInA, Set<T> onlyInB,
			Set<T> inBoth) throws HashComparisonException {
		// Map a hash code from set A to an element in set B
		// The hash codes need only distinguish unique elements within set B

		Map<Integer, T> bIdToA = new HashMap<Integer, T>();

		Set<T> unmatchedAElts = new HashSet<T>(collectionA);

		for (T b : collectionB) {
			if (bIdToA.containsKey(hashCode(b))) {
				T a = bIdToA.get(hashCode(b));
				if (equals(a, b)) {
					continue; // The elements of A and B correspond
				}

				// Problem! A member of B with the same hash code as 'b' was previously marked as equal to 'a',
				// but 'b' is not equal to 'a'.
				throw new HashComparisonException(b, hashCode(b));
			}

			boolean bEltMatched = false;
			for (T a : unmatchedAElts) {
				if (equals(a, b)) {
					unmatchedAElts.remove(a);
					bEltMatched = true;
					break;
				}
			}
			if (!bEltMatched) {
				onlyInB.add(b);
			}
			else if (inBoth != null) {
				inBoth.add(b);
			}
		}
		onlyInA.addAll(unmatchedAElts);
		return (onlyInA.size() == 0 && onlyInB.size() == 0);
	}
}
