// =============================================================================
// File        : MBlockLookupTable.java
// Author      : emok
// Change Log  : Created on Apr 26, 2007
//=============================================================================

package compling.learner.grammartables;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.util.LookupTable;

//=============================================================================

public class MBlockLookupTable extends LookupTable<TypeConstraint, TypeConstraint> {

	private static final long serialVersionUID = 5402013322510823681L;
	private Grammar grammar;

	public MBlockLookupTable(Grammar grammar) {
		super();
		this.grammar = grammar;
		for (Construction cxn : this.grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {
		TypeConstraint cxnType = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		TypeConstraint mblockType = construction.getMeaningBlock().getTypeConstraint();
		if (mblockType != null) {
			incrementCount(cxnType, mblockType, 1);
		}
	}

	public TypeConstraint lookup(TypeConstraint type, boolean unique) {
		if (unique && get(type).keySet().size() != 1) {
			throw new LearnerException("More than one values found for a unique lookup");
		}
		if (containsKey(type)) {
			return (get(type).keySet().toArray(new TypeConstraint[1]))[0];
		}
		else {
			return null;
		}
	}
}
