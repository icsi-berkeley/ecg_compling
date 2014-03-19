// =============================================================================
// File        : CooccurrenceTable.java
// Author      : emok
// Change Log  : Created on Mar 19, 2006
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.util.LookupTable;
import compling.util.MapMap;
import compling.util.Pair;

//=============================================================================

/***
 * <code>CooccurrenceTable</code> is a class that maintains a cooccurrence matrix between <code>Type</code>s. This is an
 * assymetric implementation such that the occurrence of {A, B} is not the same as that of {B, A}.
 * 
 * This class is used for keeping track of constituency structure in constructions and schemas to aid search.
 * 
 ***/

public class CooccurrenceTable<T> extends LookupTable<T, T> {
	@SuppressWarnings("unused") 
	private static final long serialVersionUID = 708238799527594449L;

	public interface QueryExpander<T> {
		public Map<T, Double> expandQuery(T a);
	}

	// private static Logger logger = Logger.getLogger(CooccurrenceTable.class.getName());

	private QueryExpander<T> queryExpander = null;

	public CooccurrenceTable() {
		super();
	}

	public void setQueryExpander(QueryExpander<T> q) {
		queryExpander = q;
	}

	public Map<Substitution<T>, Set<T>> findCoveringTypes(T type) {
		Map<T, Integer> cst = new HashMap<T, Integer>();
		cst.put(type, 1);
		return findCoveringTypes(cst);
	}

	public Map<Substitution<T>, Set<T>> findCoveringTypes(Collection<T> types) {
		Map<T, Integer> cst = new HashMap<T, Integer>();
		for (T type : types) {
			if (!cst.containsKey(type)) {
				cst.put(type, 1);
			}
			else {
				cst.put(type, cst.get(type) + 1);
			}
		}
		return findCoveringTypes(cst);
	}

	public Map<Substitution<T>, Set<T>> findCoveringTypes(Map<T, Integer> types) {

		Map<Substitution<T>, Set<T>> allCandidates = new HashMap<Substitution<T>, Set<T>>();

		Set<Pair<Map<T, Integer>, Substitution<T>>> expansions = new HashSet<Pair<Map<T, Integer>, Substitution<T>>>();
		for (T sourceType : types.keySet()) {
			expansions = expansionHelper(sourceType, types.get(sourceType), expansions);
		}

		for (Pair<Map<T, Integer>, Substitution<T>> expansion : expansions) {
			Set<T> coveringTypes = coveringTypeHelper(expansion.getFirst());
			if (!coveringTypes.isEmpty()) {
				allCandidates.put(expansion.getSecond(), coveringTypes);
			}
		}

		return allCandidates;
	}

	protected Set<Pair<Map<T, Integer>, Substitution<T>>> expansionHelper(T sourceType, int freq,
			Set<Pair<Map<T, Integer>, Substitution<T>>> expansions) {
		Set<Pair<Map<T, Integer>, Substitution<T>>> newExpansions = new HashSet<Pair<Map<T, Integer>, Substitution<T>>>();
		Map<T, Double> expandedTypes = expandQuery(sourceType);
		expandedTypes.put(sourceType, 0.0);

		if (expansions.isEmpty()) {
			for (T t : expandedTypes.keySet()) {
				Map<T, Integer> newMap = new HashMap<T, Integer>();
				Substitution<T> newSub = new Substitution<T>(sourceType);
				newMap.put(t, freq);
				newSub.substitutes(sourceType, t, expandedTypes.get(t));
				newExpansions.add(new Pair<Map<T, Integer>, Substitution<T>>(newMap, newSub));
			}
		}
		else {
			for (T t : expandedTypes.keySet()) {
				for (Pair<Map<T, Integer>, Substitution<T>> expansion : expansions) {
					Map<T, Integer> newMap = new HashMap<T, Integer>(expansion.getFirst());
					Substitution<T> newSub = new Substitution<T>(expansion.getSecond());
					if (newMap.get(t) == null) {
						newMap.put(t, freq);
					}
					else {
						newMap.put(t, newMap.get(t) + freq);
					}
					newSub.substitutes(sourceType, t, expandedTypes.get(t));
					newExpansions.add(new Pair<Map<T, Integer>, Substitution<T>>(newMap, newSub));
				}
			}
		}

		return newExpansions;

	}

	protected Set<T> coveringTypeHelper(Map<T, Integer> types) {
		Iterator<T> it = types.keySet().iterator();
		MapMap<T, T, Integer> restrictions = new MapMap<T, T, Integer>();
		Set<T> candidates = null;
		if (it.hasNext()) {
			T next = it.next();
			candidates = findCoveringTypes(next, types.get(next), restrictions);
		}
		while (it.hasNext()) {
			T next = it.next();
			Set<T> intersect = findCoveringTypes(next, types.get(next), restrictions);
			candidates.retainAll(intersect);
		}
		return candidates;
	}

	// /-------------------------------------------------------------------------

	/**
	 * This function has a side effect of modifying the restrictions map.
	 * 
	 * @param constituent
	 * @param count
	 * @param restrictions
	 * @param allowSuperTypes
	 * @return
	 */
	public Set<T> findCoveringTypes(T t, int count, MapMap<T, T, Integer> restrictions) {

		Set<T> types = new HashSet<T>();
		// types.addAll(expandQuery(t));
		types.add(t);

		Set<T> containingTypes = new HashSet<T>();
		for (T type : types) {
			if (!containsKey(type)) {
				break;
			}

			for (T candidate : get(type).keySet()) {
				if (restrictions.get(type, candidate) != null) {
					if (restrictions.get(type, candidate) >= count) {
						containingTypes.add(candidate);
						restrictions.put(t, candidate, restrictions.get(type, candidate) - count);
					}
				}
				else if (get(type, candidate) >= count) {
					containingTypes.add(candidate);
					restrictions.put(t, candidate, get(type, candidate) - count);
				}
			}
		}
		return containingTypes;
	}

	protected Map<T, Double> expandQuery(T a) {
		if (queryExpander != null) {
			return queryExpander.expandQuery(a);
		}
		else {
			return new HashMap<T, Double>();
		}
	}

	/*
	 * public Set<TypeConstraint> findLeaves(Set<TypeConstraint> types) { Set<TypeConstraint> leaves = new
	 * HashSet<TypeConstraint>(); try { for (TypeConstraint parent : types) { boolean isLeaf = true; for (TypeConstraint
	 * child : types) { if (!parent.equals(child) && parent.getTypeSystem() == child.getTypeSystem()) { isLeaf = isLeaf
	 * && !parent.getTypeSystem().subtype(child.getType().toString(), parent.getType().toString()); } } if (isLeaf) {
	 * leaves.add(parent); } } } catch (TypeSystemException tse) {
	 * logger.warning("Error encountered in TypeSystem when trying to " + "find the leaves among a set of types"); }
	 * return leaves; }
	 */

	public Collection<String> getTypeNames(Collection<T> types) {
		Collection<String> strings = new ArrayList<String>();

		for (T type : types) {
			strings.add(type.toString());
		}
		return strings;
	}

	public static void main(String[] args) {

		CooccurrenceTable<String> stats = new CooccurrenceTable<String>();

		stats.setCount("throw", "throw", 1);
		stats.setCount("ball", "ball", 1);
		stats.setCount("throw", "throw-ball", 1);
		stats.setCount("ball", "throw-ball", 1);
		stats.setCount("throw", "throw-throw-ball", 2);
		stats.setCount("ball", "throw-throw-ball", 1);

		List<String> constituents = new ArrayList<String>();
		// constituents.add("throw");
		constituents.add("throw");
		constituents.add("ball");

		Map<Substitution<String>, Set<String>> covering = stats.findCoveringTypes(constituents);

		System.out.println(covering);
	}
}
