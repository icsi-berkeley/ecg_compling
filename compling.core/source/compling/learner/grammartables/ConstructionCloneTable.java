// =============================================================================
// File        : ConstructionCloneTable.java
// Author      : emok
// Change Log  : Created on May 3, 2008
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.grammartables.SchemaCloneTable.SchemaInstance;
import compling.parser.ParserException;
import compling.parser.ecgparser.Analysis;

//=============================================================================

public class ConstructionCloneTable {

	protected Grammar grammar = null;
	Map<String, Analysis> constructionInstances = new LinkedHashMap<String, Analysis>();
	SchemaCloneTable schemaCloneTable = null;

	public ConstructionCloneTable(Grammar grammar, SchemaCloneTable schemaCloneTable) {
		this.grammar = grammar;
		this.schemaCloneTable = schemaCloneTable;
	}

	public Analysis getInstance(Construction c) {
		Analysis a = constructionInstances.get(c);
		return a != null ? a : instantiateAnalysis(c);
	}

	/**
	 * Make a "complete" semspec based on one construction, instantiating as much as possible (including constituents).
	 */
	private Analysis instantiateAnalysis(Construction c) {

		TypeSystem<Construction> cxnTypeSystem = c.getCxnTypeSystem();
		TypeSystem<Schema> schemaTypeSystem = c.getSchemaTypeSystem();
		TypeSystem<? extends TypeSystemNode> ontologyTypeSystem = c.getExternalTypeSystem();
		Role mRole = new Role(ECGConstants.MEANING_POLE);

		Analysis a = null;
		try {
			a = new Analysis(c);
		}
		catch (ParserException pe) {
			System.err.println(pe.getLocalizedMessage());
			System.err.println(c);
			a = new Analysis(c);
		}
		// is it possible to go through the constructional constituents and make schema instances
		// (by cloning off of the schema clone table) and coindexing them into this analysis?

		for (Role constituent : c.getConstructionalBlock().getElements()) {
			TypeConstraint cstMPole = cxnTypeSystem.get(constituent.getTypeConstraint().getType()).getMeaningBlock()
					.getTypeConstraint();
			if (cstMPole != null) {
				List<Role> chain = new ArrayList<Role>();
				chain.add(constituent);
				chain.add(mRole);
				SlotChain cstChain = new SlotChain().setChain(chain);

				if (cstMPole.getTypeSystem() == schemaTypeSystem) {
					// clone the schema
					SchemaInstance instance = schemaCloneTable.getInstance(schemaTypeSystem.get(cstMPole.getType()));
					a.getFeatureStructure().coindexAcrossFeatureStructureSets(cstChain, new SlotChain(""), instance);
				}
				else if (cstMPole.getTypeSystem() == ontologyTypeSystem) {
					a.getFeatureStructure().getSlot(cstChain);
				}
			}
		}
		constructionInstances.put(c.getName(), a);
		return a;
	}
}
