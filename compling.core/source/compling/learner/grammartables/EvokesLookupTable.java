// =============================================================================
// File        : EvokesLookupTable.java
// Author      : emok
// Change Log  : Created on Apr 26, 2007
//=============================================================================

package compling.learner.grammartables;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.util.LookupTable;

//=============================================================================

public class EvokesLookupTable extends LookupTable<TypeConstraint, TypeConstraint> {

	private static final long serialVersionUID = 2465360058698582441L;
	private Grammar grammar;

	public EvokesLookupTable(Grammar grammar) {
		super();
		this.grammar = grammar;
		for (Construction cxn : this.grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {
		TypeConstraint cxnType = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		for (Role evoked : construction.getMeaningBlock().getEvokedElements()) {
			incrementCount(cxnType, evoked.getTypeConstraint(), 1);
		}
	}
}
