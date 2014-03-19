package compling.parser.ecgparser.morph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Pair;

/**
 * Given a set of result analyses (SemSpec/constructional tree) produced by the parser, compare them to each other or to
 * test cases.
 * 
 * @author Nathan Schneider
 * 
 */
public class MAnalysisComparison {
	/**
	 * Given the constructional spans for the desired (correct) analysis and the analyses produced by a test run of the
	 * analyzer, determine whether the result sets seem to correspond and report any discrepancies to the error console.
	 * NOTE: Not guaranteed to catch all SemSpec errors.
	 * 
	 * @param desiredResults
	 *           Desired constructional spans as listed in the test case
	 * @param testResults
	 *           Analyses produced by running the morphological analyzer on the test input
	 * @return true iff the test results match the set of desired analyses (regardless of order)
	 * @see #matchesSemantics(Collection, Analysis, StringBuffer)
	 */
	public static boolean matchesAnalyses(List<Pair<List<CxnalSpan>, List<CSet>>> desiredResults,
			Set<MAnalysis> testResults) {

		List<MAnalysis> unmatchedTestResults = new ArrayList<MAnalysis>(testResults);

		for (Pair<List<CxnalSpan>, List<CSet>> d : desiredResults) {
			boolean foundMatch = false;
			StringBuffer mismatchLog = new StringBuffer();
			for (MAnalysis t : unmatchedTestResults) {
				if (matchesAnalysis(d, t, mismatchLog)) {
					foundMatch = true;
					unmatchedTestResults.remove(t);
					break;
				}
			}
			if (!foundMatch) {
				System.err.print(mismatchLog.toString());
				return false;
			}
		}
		return unmatchedTestResults.isEmpty();
	}

	public static boolean matchesAnalysis(Pair<List<CxnalSpan>, List<CSet>> desiredResult, MAnalysis testResult,
			StringBuffer mismatchLog) {
		Set<CxnalSpan> testSpans = new HashSet<CxnalSpan>();
		if (testResult.getSpans() == null) { // Matched an atomic construction before morph parsing
			CxnalSpan fullspan = new CxnalSpan(null, testResult.getHeadCxn(), 0, testResult.getSpanLeftIndex(),
					testResult.getSpanRightIndex(), testResult.getSpanLeftCharIndex(), testResult.getSpanRightCharIndex());
			testSpans.add(fullspan);
		}
		else
			testSpans.addAll(testResult.getSpans());

		Set<CxnalSpan> unmatchedTestSpans = new HashSet<CxnalSpan>();
		Set<CxnalSpan> unmatchedDesiredSpans = new HashSet<CxnalSpan>();
		try {
			boolean eq = CxnalSpanComparatorFactory.getComparator().compare(desiredResult.getFirst(), testSpans,
					unmatchedDesiredSpans, unmatchedTestSpans, null);
			if (!eq) {
				if (unmatchedDesiredSpans.size() > 0) {
					mismatchLog
							.append("Expected constructional spans not found in results: " + unmatchedDesiredSpans + "\n");
					if (unmatchedTestSpans.size() > 0)
						mismatchLog.append("  ");
				}
				if (unmatchedTestSpans.size() > 0)
					mismatchLog.append("Constructional spans found unexpectedly in results: " + unmatchedTestSpans + "\n");
			}
			else {
				eq = matchesSemantics(desiredResult.getSecond(), testResult, mismatchLog);
				if (!eq) {

					mismatchLog.append("Mismatch in semantic bindings");
					mismatchLog.append(".\n");
				}
			}
			return eq;
		}
		catch (HashComparator.HashComparisonException ex) {
			mismatchLog.append("SlotID mismatch: (test result) " + ex.getElt() + "\n");
			return false;
		}
	}

	/**
	 * Creates and stores a single CxnalSpanComparator instance for repeated use.
	 * 
	 * @author Nathan Schneider
	 */
	static class CxnalSpanComparatorFactory {
		static CxnalSpanComparator comparator = null;

		static CxnalSpanComparator getComparator() {
			if (comparator == null) {
				comparator = new CxnalSpanComparator();
			}
			return comparator;
		}
	}

	/**
	 * Creates and stores a single SchemaRoleComparator instance for repeated use.
	 * 
	 * @author Nathan Schneider
	 */
	static class SchemaRoleComparatorFactory {
		static SchemaRoleComparator comparator = null;

		static SchemaRoleComparator getComparator() {
			if (comparator == null) {
				comparator = new SchemaRoleComparator();
			}
			return comparator;
		}
	}

	/**
	 * Creates and stores a single CSetComparator instance for repeated use.
	 * 
	 * @author Nathan Schneider
	 */
	static class CSetComparatorFactory {
		static CSetComparator comparator = null;

		static CSetComparator getComparator() {
			if (comparator == null) {
				comparator = new CSetComparator();
			}
			return comparator;
		}
	}

	static class CxnalSpanComparator extends HashComparator<CxnalSpan> {
		public boolean equals(CxnalSpan t, CxnalSpan d) {
			return (t.getType() == d.getType() && t.getLeft() == d.getLeft() && t.getRight() == d.getRight()
					&& t.getLeftChar() == d.getLeftChar() && t.getRightChar() == d.getRightChar());
		}

		public int hashCode(CxnalSpan s) {
			return s.getSlotID();
		}
	}

	static class SchemaRoleComparator extends HashComparator<Pair<String, Role>> {
		public boolean equals(Pair<String, Role> t, Pair<String, Role> d) {
			return (t.getFirst().equals(d.getFirst()) && t.getSecond().getName().equals(d.getSecond().getName()));
		}

		public int hashCode(Pair<String, Role> s) {
			return s.hashCode(); // TODO: ?? We want different (type,role) pairs to have different codes, even if types XOR
										// roles are identical
		}
	}

	static class CSetComparator extends HashComparator<CSet> {
		public boolean equals(CSet t, CSet d) {
			return (t.equals(d));
		}

		public int hashCode(CSet s) {
			return s.slotIndex;
		}
	}

	/**
	 * Represents a set of bound constraints from an analysis, storing schema type/role names and the schema type name
	 * for the filler (if present).
	 * 
	 * @author Nathan Schneider
	 * 
	 */
	public static class CSet {
		public Set<Pair<String, Role>> schemaRoles = new HashSet<Pair<String, Role>>(); // Maps roles to types
		public int slotIndex = -1;
		public String fillerType;

		public CSet() {
		}

		public CSet(int slotIndex, String fillerType) {
			this.slotIndex = slotIndex;
			this.fillerType = fillerType;
		}

		public void addRole(String schemaType, Role role) {
			schemaRoles.add(new Pair<String, Role>(schemaType, role));
		}

		public boolean equals(CSet that) {
			if (!fillerType.equals(that.fillerType))
				return false;

			boolean eq = SchemaRoleComparatorFactory.getComparator().compare(this.schemaRoles, that.schemaRoles,
					new HashSet<Pair<String, Role>>(), new HashSet<Pair<String, Role>>(), null);
			return eq;
		}

		public String toString() {
			String s = "";
			for (Pair<String, Role> r : schemaRoles) {
				s += r.getFirst() + "." + r.getSecond() + " <--> ";
			}
			s += "Filler: " + fillerType;
			return s;
		}
	}

	/**
	 * Identifies obvious mismatches in semantic bindings (if the two analyses contain binding sets that differ in role
	 * names). Does *not* verify that schemas are coindexed properly, so it will miss some subtle errors.
	 * 
	 * @param desiredCSets
	 * @param testAnalysis
	 * @param errorLog
	 * @return
	 */
	private static boolean matchesSemantics(Collection<CSet> desiredCSets, Analysis testAnalysis, StringBuffer errorLog) {
		Collection<CSet> testCSets = getCoindexedRoleSets(testAnalysis);
		if (testCSets.size() != desiredCSets.size()) {
			errorLog.append("Mismatch in semantic bindings: " + testCSets.size() + " binding sets returned, "
					+ desiredCSets.size() + " expected.\n");
			return false;
		}
		if (desiredCSets.size() == 0)
			return true; // Both desired and test semantics are empty

		Set<CSet> onlyInDesired = new HashSet<CSet>();
		Set<CSet> onlyInTest = new HashSet<CSet>();
		boolean eq = CSetComparatorFactory.getComparator().compare(desiredCSets, testCSets, onlyInDesired, onlyInTest,
				null);
		if (!eq) {
			errorLog.append("Mismatch in semantic bindings: ");
			if (onlyInDesired.size() > 1 || onlyInTest.size() > 1)
				errorLog.append("multiple discrepancies\n");
			else {
				if (onlyInTest.size() == 1)
					errorLog.append("found binding set " + onlyInTest.iterator().next());
				if (onlyInDesired.size() == 1 && onlyInTest.size() == 1)
					errorLog.append("; ");
				if (onlyInDesired.size() == 1)
					errorLog.append("expected binding set " + onlyInDesired.iterator().next());
				errorLog.append("\n");
			}
		}
		return eq;
	}

	/**
	 * Based on AnalysisUtilities.DefaultAnalysisFormatter
	 * 
	 * @param a
	 * @return
	 */
	private static Collection<CSet> getCoindexedRoleSets(Analysis a) {
		Map<Slot, CSet> csets = new HashMap<Slot, CSet>();

		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();

			if (tc != null && tc.getTypeSystem() != a.getCxnTypeSystem()) { // then this is a schema of some sort
				if (s.getParentSlots().size() > 0
						&& compling.parser.ecgparser.morph.MGrammarWrapper.findRole(s.getParentSlots(), "f") != null) { // Some
																																						// .f
																																						// role
																																						// points
																																						// to
																																						// this;
																																						// it
																																						// must
																																						// be
																																						// a
																																						// form
																																						// schema
					// formSchemas.add(s);
				}
				else { // Must be a semantic schema
					String type = s.getTypeConstraint().getType();
					if (s.getTypeConstraint().getTypeSystem() != a.getCxnTypeSystem()
							&& s.getTypeConstraint().getTypeSystem() != a.getSchemaTypeSystem()) {
						type = "@" + type; // Ontology item
					}
					csets.put(s, new CSet(s.getID(), type));
				}
			}
		}

		// List roles/poles used in each set of semantic bindings
		for (Slot s : a.getFeatureStructure().getSlots()) {
			TypeConstraint tc = s.getTypeConstraint();
			if (tc == null) {
				continue;
			}
			String type = tc.getType();
			if (tc.getTypeSystem() != a.getCxnTypeSystem() && tc.getTypeSystem() != a.getSchemaTypeSystem()) {
				type = "@" + type;
			}
			if (s.hasStructuredFiller()) {
				for (Role role : s.getFeatures().keySet()) {
					if (csets.containsKey(s.getSlot(role))) {
						csets.get(s.getSlot(role)).addRole(type, role);
					}
				}
			}
		}

		return csets.values();
	}
}
