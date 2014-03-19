// =============================================================================
// File        : ConstituentCooccurenceTable.java
// Author      : emok
// Change Log  : Created on Jun 3, 2006
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.grammartables.SimpleExpander.ExpansionType;

//=============================================================================

public class ConstituentCooccurenceTable extends CooccurrenceTable<TypeConstraint> {

	private static final long serialVersionUID = 4534284385970639340L;

	protected Grammar grammar = null;

	public ConstituentCooccurenceTable(Grammar grammar) {
		super();
		this.grammar = grammar;

		for (Construction cxn : grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {

		TypeConstraint type = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		incrementCount(type, type, 0); // diagonals are always 1

		Set<Role> cxnConstituents = construction.getConstructionalBlock().getElements();
		for (Role cxnConstituent : cxnConstituents) {
			incrementCount(cxnConstituent.getTypeConstraint(), type, 1);
		}
	}

	public Set<Construction> findTypeWithConstituents(List<Construction> constituents) {
		ArrayList<TypeConstraint> constituentTypes = new ArrayList<TypeConstraint>();
		for (Construction cxn : constituents) {
			constituentTypes.add(grammar.getCxnTypeSystem().getCanonicalTypeConstraint(cxn.getType()));
		}
		return findTypeWithConstituents(constituentTypes);
	}

	public Set<Construction> findTypeWithConstituents(ArrayList<TypeConstraint> constituentTypes) {
		Set<Construction> constructions = new HashSet<Construction>();
		// TODO: check to see if this expansion works correctly
		setQueryExpander(new SimpleExpander(grammar, ExpansionType.Supertype));
		Map<Substitution<TypeConstraint>, Set<TypeConstraint>> cxnTypes = findCoveringTypes(constituentTypes);
		for (Set<TypeConstraint> cxnTypeSet : cxnTypes.values()) {
			for (TypeConstraint cxnType : cxnTypeSet) { // this might cause it to crash
				if (cxnType.getTypeSystem() == grammar.getCxnTypeSystem()) {
					constructions.add(grammar.getConstruction(cxnType.getType()));
				}
			}
		}
		return constructions;
	}

}
