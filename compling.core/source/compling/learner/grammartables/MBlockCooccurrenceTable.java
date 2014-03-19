// =============================================================================
// File        : MBlockCooccurrenceTable.java
// Author      : emok
// Change Log  : Created on Apr 27, 2007
//=============================================================================

package compling.learner.grammartables;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.util.LearnerUtilities;

//=============================================================================

public class MBlockCooccurrenceTable extends CooccurrenceTable<TypeConstraint> {

	private static final long serialVersionUID = 8482552754645386131L;
	protected Grammar grammar = null;
	protected GrammarTables grammarTables = null;

	public MBlockCooccurrenceTable(Grammar grammar, GrammarTables grammarTables) {
		super();
		this.grammar = grammar;
		this.grammarTables = grammarTables;
		for (Construction cxn : grammar.getAllConstructions()) {
			addTypeToStats(cxn);
		}
	}

	public void addTypeToStats(Construction construction) {
		TypeConstraint cxnType = construction.getCxnTypeSystem().getCanonicalTypeConstraint(construction.getName());
		TypeConstraint mblockType = construction.getMeaningBlock().getTypeConstraint();
		if (mblockType != null) {
			if (mblockType.getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				incrementCount(LearnerUtilities.findEventTypeRestriction(construction, grammarTables), cxnType, 1);
			}
			else {
				incrementCount(mblockType, cxnType, 1);
			}
		}
	}

}
