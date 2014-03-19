// =============================================================================
// File        : GeneralizationHistory.java
// Author      : emok
// Change Log  : Created on Jul 8, 2008
//=============================================================================

package compling.learner.learnertables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import compling.util.Indexer;
import compling.util.MapFactory.TreeMapFactory;
import compling.util.MapSet;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public class GeneralizationHistory {

	List<String> allCxns = new ArrayList<String>();
	Indexer<String> indexer = new Indexer<String>(false);
	MapSet<Integer, Integer> generalizedFrom = new MapSet<Integer, Integer>();
	MapSet<Integer, Integer> generalizedTo = new MapSet<Integer, Integer>();
	private static Logger logger = Logger.getLogger(GeneralizationHistory.class.getName());

	public GeneralizationHistory() {
	}

	public GeneralizationHistory(GeneralizationHistory that) {
		allCxns = new ArrayList<String>(that.allCxns);
		generalizedFrom = new MapSet<Integer, Integer>(that.generalizedFrom);
		generalizedTo = new MapSet<Integer, Integer>(that.generalizedTo);

		indexer = new Indexer<String>(false);
		for (String s : that.indexer) {
			indexer.add(s);
		}
	}

	public void addType(String general, Collection<String> specifics) {
		Set<Integer> specificIndices = new HashSet<Integer>();
		for (String from : specifics) {
			if (indexer.indexOf(from) == -1) {
				indexer.add(from);
				allCxns.add(from);
			}
			specificIndices.add(indexer.indexOf(from));
		}
		indexer.add(general);
		allCxns.add(general);
		int generalIndex = indexer.indexOf(general);
		for (int specificIndex : specificIndices) {
			generalizedFrom.put(generalIndex, specificIndex);
			generalizedTo.put(specificIndex, generalIndex);

			if (generalizedFrom.containsKey(specificIndex)) {
				for (int specificAncestor : generalizedFrom.get(specificIndex)) {
					generalizedFrom.put(generalIndex, specificAncestor);
					generalizedTo.put(specificAncestor, generalIndex);
				}
			}
		}
	}

	public boolean removeType(String cxn) {
		int removeIndex = indexer.indexOf(cxn);
		if (removeIndex == -1) {
			return false;
		}
		Set<Integer> origin = generalizedFrom.get(removeIndex);
		Set<Integer> furtherGeneralizations = generalizedTo.get(removeIndex);
		if (origin == null)
			origin = new HashSet<Integer>();
		if (furtherGeneralizations == null)
			furtherGeneralizations = new HashSet<Integer>();

		logger.finer("About to remove " + cxn + " from generalization history");

		for (int specific : origin) {
			generalizedTo.get(specific).remove(removeIndex);
			generalizedTo.get(specific).addAll(furtherGeneralizations);
		}
		generalizedFrom.remove(removeIndex);

		for (int general : furtherGeneralizations) {
			generalizedFrom.get(general).remove(removeIndex);
			generalizedFrom.get(general).addAll(origin);
		}
		generalizedTo.remove(removeIndex);

		allCxns.remove(cxn);
		return true;
	}

	public Set<String> getAllGeneralizations(String specific) {
		Set<String> generalizations = new HashSet<String>();
		int specificIndex = indexer.indexOf(specific);
		if (generalizedTo.containsKey(specificIndex)) {
			for (int generalization : generalizedTo.get(specificIndex)) {
				generalizations.add(indexer.get(generalization));
			}
		}
		generalizations.add(specific);
		return generalizations;
	}

	public boolean hasBeenGeneralized(String specific) {
		return generalizedFrom.containsKey(indexer.indexOf(specific));
	}

	public boolean generalizes(String general, String specific) {
		return generalizedFrom.get(indexer.indexOf(general)) == null ? false : generalizedFrom.get(
				indexer.indexOf(general)).contains(indexer.indexOf(specific));
	}

	// this is the opposite of the TypeSystem code: here I want the furthest "subtype"
	public List<String> commonGeneralizations(Collection<String> types) {
		List<String> commonGeneralizations = new ArrayList<String>();
		commonGeneralizations.addAll(allCxns);

		for (String type : types) {
			if (allCxns.contains(type)) {
				commonGeneralizations.retainAll(getAllGeneralizations(type));
			}
			else {
				// the type is not in the history, which means it has never been generalized before
				return new ArrayList<String>();
			}
		}
		return commonGeneralizations;
	}

	public boolean haveGeneralizations(String type1, String type2) {
		List<String> types = new ArrayList<String>();
		types.add(type1);
		types.add(type2);
		return !commonGeneralizations(types).isEmpty();
	}

	public String mostAggressiveGeneralization(String type1, String type2) {
		List<String> types = new ArrayList<String>();
		types.add(type1);
		types.add(type2);
		return returnOneCommonGeneralization(types, true);
	}

	public String mostAggressiveGeneralization(Collection<String> types) {
		return returnOneCommonGeneralization(types, true);
	}

	public String leastAggressiveGeneralization(String type1, String type2) {
		List<String> types = new ArrayList<String>();
		types.add(type1);
		types.add(type2);
		return returnOneCommonGeneralization(types, false);
	}

	public String leastAggressiveGeneralization(Collection<String> types) {
		return returnOneCommonGeneralization(types, false);
	}

	protected String returnOneCommonGeneralization(Collection<String> types, boolean mostCoverage) {

		List<String> common = commonGeneralizations(types);
		if (common.isEmpty())
			return null;

		MapSet<Integer, String> numGeneralizedOver = new MapSet<Integer, String>(
				new TreeMapFactory<Integer, Set<String>>(), new LinkedHashSetFactory<String>());
		for (String gen : common) {
			numGeneralizedOver.put(generalizedFrom.get(indexer.indexOf(gen)).size(), gen);
		}
		List<Integer> sizes = new ArrayList<Integer>(numGeneralizedOver.keySet());
		int wanted;
		if (mostCoverage) {
			wanted = sizes.get(sizes.size() - 1);
		}
		else {
			wanted = sizes.get(0);
		}
		return numGeneralizedOver.get(wanted).iterator().next();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (String type : allCxns) {
			if (generalizedFrom.containsKey(indexer.indexOf(type))) {
				sb.append(type).append(":\t");
				for (int from : generalizedFrom.get(indexer.indexOf(type))) {
					sb.append(indexer.get(from)).append(" ");
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		GeneralizationHistory history = new GeneralizationHistory();
		Set<String> types = new HashSet<String>();
		types.add("a");
		types.add("b");
		history.addType("gen01", types);
		System.out.println(history);

		types.clear();
		types.add("gen01");
		types.add("c");
		history.addType("gen02", types);
		System.out.println(history);

		types.clear();
		types.add("a");
		types.add("b");
		types.add("c");
		types.add("gen01");
		types.add("gen02");
		System.out.println(history.commonGeneralizations(types));
		System.out.println("mostAggressive = " + history.mostAggressiveGeneralization(types));
		System.out.println("leastAggressive = " + history.leastAggressiveGeneralization(types));

		history.removeType("gen01");
		System.out.println(history);

		// GeneralizationHistory copy = new GeneralizationHistory(history);
		// types.remove("gen01");
		// System.out.println(copy.commonGeneralizations(types));
		// System.out.println(copy);
	}

}
