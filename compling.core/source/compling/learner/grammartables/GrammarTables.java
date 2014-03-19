// =============================================================================
// File        : GrammarTables.java
// Author      : emok
// Change Log  : Created on Apr 27, 2007
//=============================================================================

package compling.learner.grammartables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.parser.ecgparser.AnalysisInContext;
import compling.parser.ecgparser.LCPGrammarWrapper;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.AnalysisInContextFactory;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.CloneTable;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.UnifyTable;

// TODO: a number of these tables are not affected by learning (assuming stable schema hierarchy).
// These include SchemaCloneTable, CoreRolesTable, and SchemaReachabilityTable.
//=============================================================================

public class GrammarTables {

	Grammar grammar;

	SchemaCloneTable schemaCloneTable = null;
	ConstructionCloneTable cxnCloneTable = null;
	CoreRolesTable coreRolesTable = null;

	ConstituentLookupTable constituentLookupTable = null;
	ConstituentCooccurenceTable constituentCooccurenceTable = null;
	MBlockCooccurrenceTable mBlockCooccurrenceTable = null;
	EvokesLookupTable evokesLookupTable = null;
	EvokesCooccurenceTable evokesCooccurenceTable;
	SchemaReachabilityTable schemaReachabilityStats = null;
	UnifyTable unifyTable = null;
	Map<Construction, Role> selfmRDLookupTable = null;

	public GrammarTables(Grammar grammar) {
		this.grammar = grammar;
		// do on-demand building of these tables. Otherwise too much time is wasted
		// between grammar revisions building tables that are not necessarily needed.
	}

	public ConstituentCooccurenceTable getConstituentCooccurenceTable() {
		if (constituentCooccurenceTable == null) {
			constituentCooccurenceTable = new ConstituentCooccurenceTable(grammar);
		}
		return constituentCooccurenceTable;
	}

	public ConstituentLookupTable getConstituentLookupTable() {
		if (constituentLookupTable == null) {
			constituentLookupTable = new ConstituentLookupTable(grammar);
		}
		return constituentLookupTable;
	}

	public EvokesCooccurenceTable getEvokesCooccurenceTable() {
		if (evokesCooccurenceTable == null) {
			evokesCooccurenceTable = new EvokesCooccurenceTable(grammar);
		}
		return evokesCooccurenceTable;
	}

	public EvokesLookupTable getEvokesLookupTable() {
		if (evokesLookupTable == null) {
			evokesLookupTable = new EvokesLookupTable(grammar);
		}
		return evokesLookupTable;
	}

	public MBlockCooccurrenceTable getMBlockCooccurrenceTable() {
		if (mBlockCooccurrenceTable == null) {
			mBlockCooccurrenceTable = new MBlockCooccurrenceTable(grammar, this);
		}
		return mBlockCooccurrenceTable;
	}

	public SchemaCloneTable getSchemaCloneTable() {
		if (schemaCloneTable == null) {
			schemaCloneTable = new SchemaCloneTable(grammar);
		}
		return schemaCloneTable;
	}

	public ConstructionCloneTable getConstructionCloneTable() {
		if (cxnCloneTable == null) {
			cxnCloneTable = new ConstructionCloneTable(grammar, getSchemaCloneTable());
		}
		return cxnCloneTable;
	}

	public CoreRolesTable getCoreRolesTable() {
		if (coreRolesTable == null) {
			coreRolesTable = new CoreRolesTable(grammar, getSchemaCloneTable());
		}
		return coreRolesTable;
	}

	public SchemaReachabilityTable getSchemaReachabilityTable() {
		if (schemaReachabilityStats == null) {
			schemaReachabilityStats = new SchemaReachabilityTable(grammar, getSchemaCloneTable());
		}
		return schemaReachabilityStats;
	}

	public UnifyTable getUnifyTable() {
		if (unifyTable == null) {
			AnalysisInContextFactory analysisFactory = new AnalysisInContextFactory(new LCPGrammarWrapper(grammar),
					grammar.getContextModel().getContextModelCache());
			CloneTable<AnalysisInContext> cloneTable = new CloneTable<AnalysisInContext>(new LCPGrammarWrapper(grammar),
					analysisFactory);
			unifyTable = new UnifyTable(new LCPGrammarWrapper(grammar), cloneTable);
		}
		return unifyTable;
	}

	public Map<Construction, Role> getSelfmRDLookupTable() {

		if (selfmRDLookupTable == null) {
			selfmRDLookupTable = buildRDLookupTable();
		}
		return selfmRDLookupTable;
	}

	private Map<Construction, Role> buildRDLookupTable() {
		Map<Construction, Role> constituentsWithRD = new HashMap<Construction, Role>();
		for (Construction constituent : grammar.getAllConstructions()) {
			Set<Role> evoked = constituent.getMeaningBlock().getEvokedElements();
			Role rdRole = null;
			for (Role e : evoked) {
				if (e.getTypeConstraint().getType().equals(ECGConstants.RD)) {
					boolean isSelfM = false;
					for (Constraint c : constituent.getMeaningBlock().getConstraints()) {
						if (c.getArguments().contains(new ECGSlotChain(e.getName() + "." + ECGConstants.RESOLVEDREFERENT))
								&& c.getArguments().contains(
										new ECGSlotChain(ECGConstants.SELF + "." + ECGConstants.MEANING_POLE))) {
							isSelfM = true;
							break;
						}
					}
					if (isSelfM) {
						rdRole = e;
						break;
					}
				}
			}
			if (rdRole != null) {
				constituentsWithRD.put(constituent, rdRole);
			}
		}
		return constituentsWithRD;
	}
}
