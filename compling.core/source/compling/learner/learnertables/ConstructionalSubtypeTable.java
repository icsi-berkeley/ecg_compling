// =============================================================================
//File        : ConstructionalSubtypeLookupTable.java
//Author      : emok
//Change Log  : Created on Dec 7, 2007
//=============================================================================

package compling.learner.learnertables;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser.ParamContainer;
import compling.util.LookupTable;
import compling.util.Pair;
import compling.util.fileutil.TextFileLineIterator;

//LookupTable<Pair<constructional type, constructional constituents>, filler constructional type>

public class ConstructionalSubtypeTable {

	public static enum Locality {
		LOCAL, NONLOCAL, OMITTED, UNFILLED;
	}

	private static final long serialVersionUID = 4664931446963886998L;
	private Grammar currentGrammar;
	private TypeSystem<Construction> cxnTypeSystem = null;

	LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> expansionTable = new LookupTable<Pair<TypeConstraint, Role>, TypeConstraint>();
	LookupTable<Pair<TypeConstraint, Role>, Locality> localityTable = new LookupTable<Pair<TypeConstraint, Role>, Locality>();

	public ConstructionalSubtypeTable(Grammar grammar) {
		super();
		currentGrammar = grammar;
		cxnTypeSystem = currentGrammar.getCxnTypeSystem();
	}

	public ConstructionalSubtypeTable(Grammar grammar, TextFileLineIterator subtypeTableIterator,
			TextFileLineIterator localityTableIterator) {
		this(grammar);
		while (subtypeTableIterator.hasNext()) {
			String line = subtypeTableIterator.next();
			if (line == "") {
				continue;
			}
			ParamContainer pc = ParamLineParser.parseLine(line);
			TypeConstraint constructionType = getCurrentTypeConstraint(pc.structureName);
			Role constituent = getCurrentRole(constructionType, pc.role);

			if (constructionType != null && constituent != null) {
				Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(constructionType, constituent);
				for (Pair<String, Double> pair : pc.params) {
					TypeConstraint fillerType = getCurrentTypeConstraint(pair.getFirst());
					if (fillerType != null) {
						expansionTable.incrementCount(newSlot, fillerType, pair.getSecond().intValue());
					}
				}
			}
		}

		while (localityTableIterator.hasNext()) {
			String line = localityTableIterator.next();
			if (line == "") {
				continue;
			}
			StringTokenizer st = new StringTokenizer(line);
			String tmp = st.nextToken();
			String[] structureAndRole = tmp.split("\\.");
			TypeConstraint constructionType = getCurrentTypeConstraint(structureAndRole[0]);
			Role constituent = getCurrentRole(constructionType, structureAndRole[1]);

			if (constructionType != null && constituent != null) {
				Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(constructionType, constituent);
				for (Locality locality : Locality.values()) {
					if (!st.hasMoreTokens()) {
						throw new LearnerException(
								"Error in parameter file format: insufficient locality parameters provided for "
										+ structureAndRole[0] + "." + structureAndRole[1]);
					}
					String token = st.nextToken();
					localityTable.incrementCount(newSlot, locality, Integer.valueOf(token));
				}
			}
		}
	}

	public ConstructionalSubtypeTable(Grammar grammar, ConstructionalSubtypeTable oldTable) {
		this(grammar);

		// this disregard any entry in the oldTable that refers to cxns that don't exist in the new grammar
		for (Pair<TypeConstraint, Role> oldSlot : oldTable.expansionTable.keySet()) {
			TypeConstraint newConstructionType = getCurrentTypeConstraint(oldSlot.getFirst().getType());
			Role newConstituent = getCurrentRole(newConstructionType, oldSlot.getSecond().getName());

			if (newConstructionType != null && newConstituent != null) {
				Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(newConstructionType, newConstituent);
				for (TypeConstraint oldFiller : oldTable.expansionTable.get(oldSlot).keySet()) {
					TypeConstraint newFiller = getCurrentTypeConstraint(oldFiller.getType());
					if (newFiller != null) {
						expansionTable.incrementCount(newSlot, newFiller, oldTable.expansionTable.get(oldSlot, oldFiller));
					}
				}
			}
		}

		for (Pair<TypeConstraint, Role> oldSlot : oldTable.localityTable.keySet()) {
			TypeConstraint newFrameTC = getCurrentTypeConstraint(oldSlot.getFirst());
			Role newRole = newFrameTC == null ? null : getCurrentRole(oldSlot.getFirst(), oldSlot.getSecond().getName());

			if (newFrameTC != null && newRole != null) {
				Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(newFrameTC, newRole);
				for (Locality loc : oldTable.localityTable.get(oldSlot).keySet()) {
					localityTable.incrementCount(newSlot, loc, oldTable.localityTable.get(oldSlot, loc));
				}
			}
		}
	}

	public void clear() {
		expansionTable.clear();
		localityTable.clear();
	}

	private TypeConstraint getCurrentTypeConstraint(TypeConstraint oldType) {
		return cxnTypeSystem.getCanonicalTypeConstraint(oldType.getType());
	}

	private TypeConstraint getCurrentTypeConstraint(String oldType) {
		return cxnTypeSystem.getCanonicalTypeConstraint(oldType);
	}

	private Role getCurrentRole(TypeConstraint cxnType, String constituentName) {
		return cxnType == null ? null : currentGrammar.getConstruction(cxnType.getType()).getConstructionalBlock()
				.getRole(constituentName);
	}

	public LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> getExpansionTable() {
		return expansionTable;
	}

	public LookupTable<Pair<TypeConstraint, Role>, Locality> getLocalityTable() {
		return localityTable;
	}

	public TypeSystem<Construction> getCxnTypeSystem() {
		return cxnTypeSystem;
	}

	public void addOmissionInformation(String type, Role role, int omissionCount, int localCount) {
		TypeConstraint cxnType = getCurrentTypeConstraint(type);
		Role cxnRole = getCurrentRole(cxnType, role.getName());
		Pair<TypeConstraint, Role> constituent = new Pair<TypeConstraint, Role>(cxnType, cxnRole);
		localityTable.setCount(constituent, Locality.OMITTED, omissionCount);
		localityTable.setCount(constituent, Locality.LOCAL, localCount);
	}

	public void updateTable(LearnerCentricAnalysis lca, boolean learningMode) {
		LCATables tables = lca.getTables();

		Map<Integer, CxnalSpan> spans = tables.getAllCxnalSpans();

		for (Integer slotID : tables.getAllCxnSlots()) {
			Slot cxnSlot = lca.getSlot(slotID);
			TypeConstraint cxnType = cxnSlot.getTypeConstraint();
			if (!learningMode || !cxnType.getType().equals(ECGConstants.ROOT)) {
				if (cxnSlot.hasStructuredFiller()) {
					for (Role r : cxnSlot.getFeatures().keySet()) {
						Slot cstSlot = cxnSlot.getSlot(r);
						TypeConstraint roleTC = cstSlot.getTypeConstraint();
						if (roleTC != null && roleTC.getTypeSystem().getName() == ECGConstants.CONSTRUCTION
								&& !roleTC.getType().equals(ChildesLocalizer.LEFTOVER_MORPHEME)) {
							CxnalSpan span = spans.get(cstSlot.getID());
							Pair<TypeConstraint, Role> chain = new Pair<TypeConstraint, Role>(cxnType, r);
							if (span == null) {
								// this is an optional slot that isn't filled
								localityTable.incrementCount(chain, Locality.UNFILLED, 1);
							}
							else if (span.omitted()) {
								// this is an omitted argument
								localityTable.incrementCount(chain, Locality.OMITTED, 1);
							}
							else if (span.gappedOut()) {
								localityTable.incrementCount(chain, Locality.NONLOCAL, 1);
							}
							else {
								localityTable.incrementCount(chain, Locality.LOCAL, 1);
								expansionTable.incrementCount(chain, roleTC, 1);
							}
						}
					}
				}
			}
		}
	}

	public void outputConstituentExpansionCountTable(File output) throws IOException {
		PrintStream ps = new PrintStream(output);
		ps.print(outputConstituentExpansionCountTable().toString());
		ps.close();
	}

	public StringBuffer outputConstituentExpansionCountTable() {
		StringBuffer sb = new StringBuffer();
		List<Pair<TypeConstraint, Role>> constituents = new ArrayList<Pair<TypeConstraint, Role>>(expansionTable.keySet());
		Collections.sort(constituents, new ChainComparator());
		for (Pair<TypeConstraint, Role> constituent : constituents) {
			String roleChain = constituent.getFirst().getType() + "." + constituent.getSecond().getName();
			sb.append(roleChain + "\t");
			for (TypeConstraint fillerType : expansionTable.get(constituent).keySet()) {
				try {
					sb.append(fillerType.getType() + ":" + expansionTable.get(constituent, fillerType) + "\t");
				}
				catch (NullPointerException npe) {
					System.err.println("naze?");
				}
			}
			sb.append("\n");
		}
		return sb;
	}

	public void outputConstituentLocalityCostTable(File output) throws IOException {
		PrintStream ps = new PrintStream(output);
		ps.print(outputConstituentLocalityCostTable().toString());
		ps.close();
	}

	public StringBuffer outputConstituentLocalityCostTable() {
		StringBuffer sb = new StringBuffer();
		List<Pair<TypeConstraint, Role>> constituents = new ArrayList<Pair<TypeConstraint, Role>>(localityTable.keySet());
		Collections.sort(constituents, new ChainComparator());
		for (Pair<TypeConstraint, Role> constituent : constituents) {
			String roleChain = constituent.getFirst().getType() + "." + constituent.getSecond().getName();
			sb.append(roleChain + "\t");
			for (Locality locality : Locality.values()) {
				if (localityTable.get(constituent, locality) == null) {
					sb.append("0\t");
				}
				else {
					sb.append(localityTable.get(constituent, locality) + "\t");
				}
			}
			sb.append("\n");
		}
		return sb;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(outputConstituentExpansionCountTable()).append("\n");
		sb.append(outputConstituentLocalityCostTable()).append("\n");
		return sb.toString();
	}

	public static class ChainComparator implements Comparator<Pair<TypeConstraint, Role>> {
		public int compare(Pair<TypeConstraint, Role> p1, Pair<TypeConstraint, Role> p2) {
			return p1.getFirst().getType().compareTo(p2.getFirst().getType()) != 0 ? p1.getFirst().getType()
					.compareTo(p2.getFirst().getType()) : p1.getSecond().getName().compareTo(p2.getSecond().getName());
		}
	}

}
