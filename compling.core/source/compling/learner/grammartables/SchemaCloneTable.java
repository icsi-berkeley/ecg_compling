// =============================================================================
// File        : SchemaCloneTable.java
// Author      : emok
// Change Log  : Created on Feb 13, 2008
//=============================================================================

package compling.learner.grammartables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.featurestructure.MultiRootedSlotChainTracker;
import compling.parser.ParserException;
import compling.util.Pair;

//=============================================================================

public class SchemaCloneTable {

	static int uniqueID = 0;

	/**
	 * 
	 * NOTE: the coindexed chains returned by this clone table has an extra prefix for the root slot (unlike the slot
	 * chain tracker used by the analyzer)
	 * 
	 * This is a slimmed-down version of LCA + LCATables for the sole purpose of making an accurate representation of
	 * schemas in terms of available binding slots.
	 * 
	 * @author emok
	 */
	public static class SchemaInstance extends FeatureStructureSet {

		Schema schema = null;
		MultiRootedSlotChainTracker tracker = null;
		Map<Slot, List<SlotChain>> slotChainTable = new HashMap<Slot, List<SlotChain>>();

		public SchemaInstance(TypeConstraint tc) {
			super(tc);
			Object obj = tc.getTypeSystem().get(tc.getType());
			if (obj instanceof Schema) {
				schema = (Schema) obj;
			}
			else {
				throw new LearnerException("A SchemaInstance should not be created from a non-schema: " + tc.getType());
			}
		}

		public List<SlotChain> getCoindexedRoles(String roleName) {
			if (tracker == null) {
				buildSlotChains();
			}
			Slot roleSlot = getSlot(new SlotChain(roleName));
			return roleSlot == null ? null : slotChainTable.get(roleSlot);
		}

		public List<SlotChain> getAllCoindexedSlotChainsBySlot(Slot slot) {
			if (tracker == null) {
				buildSlotChains();
			}
			return slotChainTable.get(slot);
		}

		protected void buildSlotChains() {
			tracker = new MultiRootedSlotChainTracker(this);
			for (Slot slot : getSlots()) {
				List<SlotChain> slotChains = new ArrayList<SlotChain>();
				for (Pair<Slot, SlotChain> sc : tracker.getAllCoindexedSlotChainsBySlot(slot)) {
					if (sc.getFirst() == getMainRoot()) {
						slotChains.add(sc.getSecond());
					}
					else {
						throw new LearnerException("There shouldn't be multiple root slots inside a schema instance");
					}
				}
				slotChainTable.put(slot, slotChains);
			}
		}

		public Set<Slot> getNonRootSlots() {
			Set<Slot> nonRootSlots = new HashSet<Slot>(getSlots());
			nonRootSlots.removeAll(getRootSlots());
			return nonRootSlots;
		}
	}

	protected Grammar grammar = null;
	Map<String, SchemaInstance> schemaInstances = new HashMap<String, SchemaInstance>();

	public SchemaCloneTable(Grammar grammar) {
		this.grammar = grammar;

		for (Schema schema : grammar.getAllSchemas()) {
			instantiateSchema(schema);
		}
	}

	public SchemaInstance getInstance(Schema schema) {
		SchemaInstance s = schemaInstances.get(schema.getName());
		return s != null ? s : instantiateSchema(schema);
	}

	protected SchemaInstance instantiateSchema(Schema schema) {
		TypeConstraint type = grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(schema.getName());
		SchemaInstance instance = new SchemaInstance(grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(
				schema.getType()));
		addRoles(instance, new ArrayList<Role>(), type);
		for (Slot slot : instance.getSlots()) {
			slot.setID(uniqueID++);
		}
		schemaInstances.put(schema.getType(), instance);
		return instance;
	}

	protected void addRoles(FeatureStructureSet fss, List<Role> chain, TypeConstraint type) {
		fss.getSlot(new SlotChain().setChain(chain));
		if (type != null) {
			if (type.getTypeSystem() == grammar.getSchemaTypeSystem()) {
				Schema schema = grammar.getSchemaTypeSystem().get(type.getType().toString());
				for (Role role : schema.getAllRoles()) {
					List<Role> roles = new ArrayList<Role>(chain);
					roles.add(role);
					addRoles(fss, roles, role.getTypeConstraint());
				}

				// Add in coindexation constraints
				for (Constraint constraint : schema.getContents().getConstraints()) {
					if (!constraint.overridden()) {
						if (addConstraint(fss, constraint, chain, "") == false) {
							throw new LearnerException(
									"Error in building schema reachability table because of the constraint " + constraint);
						}
					}
				}

				// We also know exactly the slot chain to get to the role. So cache it?
			}
		}
	}

	private boolean addConstraint(FeatureStructureSet fss, Constraint constraint, List<Role> prepend, String prefix) {
		if (constraint.isAssign()) {
			List<Role> sc = new ArrayList<Role>(prepend);
			sc.addAll(constraint.getArguments().get(0).getChain());
			SlotChain sc0 = new SlotChain().setChain(sc);
			return addConstraint(fss, new Constraint(constraint.getOperator(), sc0, constraint.getValue()), prefix);
		}
		else {
			SlotChain sc0 = constraint.getArguments().get(0);
			SlotChain sc1 = constraint.getArguments().get(1);
			if (!sc0.toString().equalsIgnoreCase(ECGConstants.SELF)) {
				List<Role> sc = new ArrayList<Role>(prepend);
				sc.addAll(sc0.getChain());
				sc0 = new SlotChain().setChain(sc);
			}
			if (!sc1.toString().equalsIgnoreCase(ECGConstants.SELF)) {
				List<Role> sc = new ArrayList<Role>(prepend);
				sc.addAll(sc1.getChain());
				sc1 = new SlotChain().setChain(sc);
			}
			return addConstraint(fss, new Constraint(constraint.getOperator(), sc0, sc1), prefix);
		}
	}

	private boolean addConstraint(FeatureStructureSet fss, Constraint constraint, String prefix) {
		if (constraint.getOperator().equals(ECGConstants.ASSIGN)) {
			if (constraint.getValue().charAt(0) == '\"') {
				return fss.fill(new ECGSlotChain(prefix, constraint.getArguments().get(0)), constraint.getValue());
			}
			else if (constraint.getValue().charAt(0) != '\"') {// then this is a type
				TypeConstraint typeConstraint = null;
				if (constraint.getValue().charAt(0) == '@') {
					typeConstraint = grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(
							constraint.getValue().substring(1));
				}
				else {
					typeConstraint = grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(constraint.getValue());
				}
				if (typeConstraint == null) {
					throw new ParserException("Type " + constraint.getValue() + " in constraint " + constraint
							+ " is undefined");
				}
				fss.getSlot(new ECGSlotChain(prefix, constraint.getArguments().get(0))).setTypeConstraint(typeConstraint);
			}
			return true;
		}
		else if (constraint.getOperator().equals(ECGConstants.IDENTIFY)) {
			SlotChain sc0 = constraint.getArguments().get(0);
			SlotChain sc1 = constraint.getArguments().get(1);
			if (!sc0.getChain().isEmpty() && sc0.getChain().get(0) != ECGConstants.DSROLE) {
				sc0 = new ECGSlotChain(prefix, sc0);
			}
			if (!sc1.getChain().isEmpty() && sc1.getChain().get(0) != ECGConstants.DSROLE) {
				sc1 = new ECGSlotChain(prefix, sc1);
			}
			return fss.coindex(sc0, sc1);
		}
		else {
			throw new ParserException("Analysis does not support a " + constraint.getOperator() + " constraint");
		}
	}
}
