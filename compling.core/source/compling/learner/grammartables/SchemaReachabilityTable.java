// =============================================================================
//File        : SchemaReachabilityTable.java
//Author      : emok
//Change Log  : Created on Nov 12, 2006
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.List;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.grammartables.SchemaCloneTable.SchemaInstance;

//=============================================================================

public class SchemaReachabilityTable extends CooccurrenceTable<TypeConstraint> {
	private static final long serialVersionUID = 7691671739848010452L;

	protected Grammar grammar = null;
	protected SchemaCloneTable cloneTable = null;
	protected TypeSystem<Schema> schemaTypeSystem = null;

	public SchemaReachabilityTable(Grammar grammar, SchemaCloneTable cloneTable) {
		super();
		this.grammar = grammar;
		this.cloneTable = cloneTable;
		schemaTypeSystem = grammar.getSchemaTypeSystem();

		for (Schema schema : grammar.getAllSchemas()) {
			addTypeToStats(schema);
		}
	}

	protected void addTypeToStats(Schema schema) {
		TypeConstraint type = schemaTypeSystem.getCanonicalTypeConstraint(schema.getName());
		for (Slot slot : cloneTable.getInstance(schema).getNonRootSlots()) {
			if (slot.getTypeConstraint() != null) {
				incrementCount(slot.getTypeConstraint(), type, 1);
			}
		}
	}

	public List<List<SlotChain>> getPathToRoleWithType(TypeConstraint schemaType, TypeConstraint roleType) {
		if (schemaType.getTypeSystem() != schemaTypeSystem) {
			return null;
		}

		List<List<SlotChain>> paths = new ArrayList<List<SlotChain>>();
		SchemaInstance instance = cloneTable.getInstance(schemaTypeSystem.get(schemaType.getType()));
		for (Slot slot : instance.getNonRootSlots()) {
			if (slot.getTypeConstraint() != null && slot.getTypeConstraint().equals(roleType)) {
				paths.add(instance.getAllCoindexedSlotChainsBySlot(slot));
			}
		}
		return paths;
	}
}
