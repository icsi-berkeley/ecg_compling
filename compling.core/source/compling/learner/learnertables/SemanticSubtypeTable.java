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
import java.util.Set;

import compling.context.MiniOntology.Type;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.util.LookupTable;
import compling.util.Pair;

//=============================================================================

public class SemanticSubtypeTable extends LookupTable<Pair<TypeConstraint, Role>, TypeConstraint> {
	private static final long serialVersionUID = 4664931446963886998L;
	
	private Grammar currentGrammar;
	private TypeSystem<? extends TypeSystemNode> ontologyTypeSystem;
	private TypeSystem<Schema> schemaTypeSystem;

	// private MapMap<String, String, Double> subtypeProbabilities = new MapMap<String, String, Double>();

	public SemanticSubtypeTable(Grammar grammar) {
		super();
		this.currentGrammar = grammar;
		schemaTypeSystem = currentGrammar.getSchemaTypeSystem();
		ontologyTypeSystem = currentGrammar.getOntologyTypeSystem();
	}

	public SemanticSubtypeTable(Grammar grammar, SemanticSubtypeTable oldTable) {
		this(grammar);

		TypeSystem<? extends TypeSystemNode> oldOntTypeSystem = oldTable.currentGrammar.getOntologyTypeSystem();
		TypeSystem<Schema> oldSchemaTypeSystem = oldTable.currentGrammar.getSchemaTypeSystem();

		for (Pair<TypeConstraint, Role> oldSlot : oldTable.keySet()) {
			TypeConstraint newFrameTC = schemaTypeSystem.getCanonicalTypeConstraint(oldSlot.getFirst().getType());
			Role newRole = newFrameTC == null ? null : currentGrammar.getSchema(oldSlot.getFirst().getType()).getRole(
					oldSlot.getSecond().getName());

			if (newFrameTC != null && newRole != null) {
				Pair<TypeConstraint, Role> newSlot = new Pair<TypeConstraint, Role>(newFrameTC, newRole);
				for (TypeConstraint oldFiller : oldTable.get(oldSlot).keySet()) {

					TypeConstraint newFiller = null;
					if (oldFiller.getTypeSystem() == oldSchemaTypeSystem) {
						newFiller = schemaTypeSystem.getCanonicalTypeConstraint(oldFiller.getType());
					}
					else if (oldFiller.getTypeSystem() == oldOntTypeSystem) {
						newFiller = ontologyTypeSystem.getCanonicalTypeConstraint(oldFiller.getType());
					}

					if (newFiller != null) {
						incrementCount(newSlot, newFiller, oldTable.get(oldSlot, oldFiller));
					}
				}
			}
		}
	}

	public void clear() {
		super.clear();
	}

	public void updateTable(LearnerCentricAnalysis lca) {
		LCATables tables = lca.getTables();
		Set<Integer> dummyFillers = tables.getIgnoreSlots();

		for (Integer slotID : tables.getAllNonCxnSlots()) {
			Slot semanticSlot = lca.getSlot(slotID);
			if (!dummyFillers.contains(semanticSlot) && semanticSlot.hasStructuredFiller()) {
				for (Role r : semanticSlot.getFeatures().keySet()) {
					Slot fillerSlot = semanticSlot.getSlot(r);
					if (!dummyFillers.contains(fillerSlot) && fillerSlot.getTypeConstraint() != null
							&& r.getTypeConstraint() != null) {
						this.incrementCount(new Pair<TypeConstraint, Role>(semanticSlot.getTypeConstraint(), r),
								fillerSlot.getTypeConstraint(), 1);
					}
				}

			}
		}
	}

	/*
	 * public void buildTables() { for (Pair<TypeConstraint, Role> role : keySet()) { String roleChain =
	 * role.getFirst().getType() + "." + role.getSecond().getName(); TypeConstraint roleTC =
	 * role.getSecond().getTypeConstraint(); try { List<String> subtypes = new
	 * ArrayList<String>(roleTC.getTypeSystem().getAllSubtypes(roleTC.getType())); tabulate(true, subtypeProbabilities,
	 * roleChain, subtypes, get(role), roleTC.getTypeSystem() == ontologyTypeSystem); } catch (TypeSystemException tse) {
	 * throw new
	 * LearnerException("In building semantic subtype table, Type system exception encountered looking up subtypes of " +
	 * roleTC.getType()); } } }
	 * 
	 * protected void tabulate(boolean smooth, MapMap<String, String, Double> constituentProbs, String itemName,
	 * List<String> subtypes, Map<TypeConstraint, Integer> counts, boolean externalTypeSystem) {
	 * 
	 * String prefix = externalTypeSystem ? "@" : "";
	 * 
	 * List<String> observedTypes = new ArrayList<String>(); for (TypeConstraint observedType : counts.keySet()) {
	 * observedTypes.add(observedType.getType()); } subtypes.removeAll(observedTypes); // this is the number of
	 * unobserved types
	 * 
	 * int N = 0; for (int count : counts.values()) { N += count; }
	 * 
	 * if (!smooth || subtypes.size() == 0) { // no reason to smooth if there is only one allowable type by the grammar
	 * for (TypeConstraint observedType : counts.keySet()) { double prob = ((double) counts.get(observedType)) / N;
	 * constituentProbs.put(itemName, prefix + observedType.getType(), prob); }
	 * 
	 * } else { // Witten-Bell discounting (see Jurafsky & Martin p. 211 eq 6.18 & 6.19) // T: # of observed types // N:
	 * # of observed tokens // Z: # of unobserved types int T = counts.size(); int Z = subtypes.size() - T;
	 * 
	 * for (TypeConstraint observedType : counts.keySet()) { double smoothedProb = ((double) counts.get(observedType)) /
	 * (N + T); constituentProbs.put(itemName, prefix + observedType.getType(), smoothedProb); }
	 * 
	 * for (String unobservedType : subtypes) { double smoothedProb = ((double) T) / (Z *(N + T));
	 * constituentProbs.put(itemName, prefix + unobservedType, smoothedProb); } } }
	 */

	public void outputSemanticFillerCostTable(File output) throws IOException {
		PrintStream ps = new PrintStream(output);
		ps.print(outputSemanticFillerCostTable().toString());
		ps.close();
	}

	public StringBuffer outputSemanticFillerCostTable() {
		StringBuffer sb = new StringBuffer();
		List<Pair<TypeConstraint, Role>> constituents = new ArrayList<Pair<TypeConstraint, Role>>(keySet());
		Collections.sort(constituents, new ChainComparator());
		for (Pair<TypeConstraint, Role> constituent : constituents) {
			sb.append(constituent.getFirst().getType()).append(".").append(constituent.getSecond().getName()).append("\t");
			for (TypeConstraint constraint : get(constituent).keySet()) {
				if (constraint.getTypeSystem() == ontologyTypeSystem) {
					sb.append("@");
				}
				sb.append(constraint.getType()).append(":").append(get(constituent, constraint)).append("\t");
			}
			sb.append("\n");
		}
		return sb;
	}

	public String toString() {
		return outputSemanticFillerCostTable().toString();
	}

	public static class ChainComparator implements Comparator<Pair<TypeConstraint, Role>> {
		public int compare(Pair<TypeConstraint, Role> p1, Pair<TypeConstraint, Role> p2) {
			return p1.getFirst().getType().compareTo(p2.getFirst().getType()) != 0 ? p1.getFirst().getType()
					.compareTo(p2.getFirst().getType()) : p1.getSecond().getName().compareTo(p2.getSecond().getName());
		}
	}
}
