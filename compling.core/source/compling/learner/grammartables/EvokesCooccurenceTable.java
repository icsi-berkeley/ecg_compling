// =============================================================================
// File        : EvokesCooccurenceTable.java
// Author      : emok
// Change Log  : Created on Apr 27, 2007
//=============================================================================

package compling.learner.grammartables;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;

//=============================================================================

public class EvokesCooccurenceTable extends CooccurrenceTable<TypeConstraint> {

	private static final long serialVersionUID = -452089753174009029L;
	protected Grammar grammar = null;

	public EvokesCooccurenceTable(Grammar grammar) {
		super();
		this.grammar = grammar;
		for (Construction cxn : grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {
		TypeConstraint cxnType = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		for (Role evoked : construction.getMeaningBlock().getEvokedElements()) {
			incrementCount(evoked.getTypeConstraint(), cxnType, 1);
		}
	}

}
