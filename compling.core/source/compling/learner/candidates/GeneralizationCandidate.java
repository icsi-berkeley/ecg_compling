// =============================================================================
// File        : GeneralizationCandidate.java
// Author      : emok
// Change Log  : Created on Mar 12, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.grammartables.GrammarTables;
import compling.learner.grammartables.SchemaCloneTable.SchemaInstance;
import compling.learner.util.LearnerUtilities;
import compling.learner.util.LearnerUtilities.EqualsMappingFunction;
import compling.parser.ecgparser.Analysis;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

// each generalization opeation not only creates at least two new constructions --
// an abstract constructional category and a cxn that uses that category
// but it also modifies existing one by making them members of the new category.
// idea: clean up operations might be needed after every sweep to get merge categories.

public abstract class GeneralizationCandidate {
	// NOTE: this should be restricted to pairs of typeconstraints, but lists are easier to use.

	boolean isViable = false;

	private static final long serialVersionUID = 1L;

	TypeConstraint sourceType = null;
	Construction sourceCxn = null;
	TypeConstraint targetType = null;
	Construction targetCxn = null;

	String newCxnName = null;

	static Role mPole = new Role(ECGConstants.MEANING_POLE);

	LearnerGrammar learnerGrammar;
	Grammar grammar;
	GrammarTables grammarTables;
	TypeSystem<Construction> cxnTypeSystem;
	MapSet<String, String> potentialCategoriesToMerge = new MapSet<String, String>();

	Set<Construction> additionsToGrammar = new LinkedHashSet<Construction>();
	Map<Construction, Construction> substitutionToGrammar = new LinkedHashMap<Construction, Construction>();
	Set<Construction> removalFromGrammar = new LinkedHashSet<Construction>();
	Map<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>> finalRoleMapping = new HashMap<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>>();

	public void createNewConstructions() {
		try {
			Set<String> collectiveParents = new HashSet<String>();
			collectiveParents.addAll(sourceCxn.getParents());
			collectiveParents.addAll(targetCxn.getParents());
			Collection<String> parents = cxnTypeSystem.allBestCommonSupertypes(collectiveParents);
			if (parents.isEmpty()) { // NOTE: I think this only works because there is a root type in the starter grammar
				throw new LearnerException("Generalization failed: can't find parent type for the general construction");
			}
			createNewConstructions(new HashSet<String>(parents));
		}
		catch (TypeSystemException tse) {
			throw new LearnerException("TypeSystemException when creating new generalized construction", tse);
		}
	}

	public GrammarChanges getChanges() {
		return new GrammarChanges(additionsToGrammar, substitutionToGrammar, removalFromGrammar);
	}

	abstract protected void createNewConstructions(Set<String> parents) throws TypeSystemException;

	protected void relaxConstraint(Constraint constraint, String relaxedRestriction) {
		assert (constraint.getOperator().equals(ECGConstants.ASSIGN) && constraint.getValue().charAt(0) != ECGConstants.CONSTANTFILLERPREFIX);
		constraint.setValue(constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX ? ECGConstants.ONTOLOGYPREFIX
				+ relaxedRestriction : relaxedRestriction);
	}

	public static Set<String> findRelaxedContextualConstraint(Grammar grammar, Constraint constraint, Analysis semspec) {

		TypeConstraint givenConstraint = null;
		if (constraint.getValue().charAt(0) == ECGConstants.CONSTANTFILLERPREFIX) {
			// this is a constant (String) filler -- there's nothing that can be done about it.
			return null;
		}
		else {
			givenConstraint = constraint.getValue().charAt(0) == ECGConstants.ONTOLOGYPREFIX ? grammar
					.getOntologyTypeSystem().getCanonicalTypeConstraint(constraint.getValue().substring(1)) : grammar
					.getSchemaTypeSystem().getCanonicalTypeConstraint(constraint.getValue());
		}

		TypeSystem<?> ts = givenConstraint.getTypeSystem();
		SlotChain slotChain = constraint.getArguments().get(0);
		TypeConstraint actualConstraint = semspec.getFeatureStructure().getSlot(slotChain).getTypeConstraint();

		if (ts != actualConstraint.getTypeSystem()) {
			return null;
		} // see if this breaks. Not sure.

		Set<String> children = new HashSet<String>();
		children.add(ts.getInternedString(givenConstraint.getType()));
		children.add(ts.getInternedString(actualConstraint.getType()));
		try {
			List<String> relaxedTypes = givenConstraint.getTypeSystem().allBestCommonSupertypes(children);
			return new HashSet<String>(relaxedTypes);
		}
		catch (TypeSystemException tse) {
			return null;
		}
	}

	protected List<Constraint> liftConstraints(TypeConstraint newMPoleType, Construction refCxn,
			Map<Role, String> newConstituentTypes, Set<String> parentsOfGeneralCxn) {
		List<Constraint> refConstraints = new ArrayList<Constraint>();
		Block refBlock = refCxn.getMeaningBlock();
		if (newMPoleType != null && newMPoleType.getTypeSystem().getName().equals(ECGConstants.SCHEMA)) {
			SchemaInstance refInstance = grammarTables.getSchemaCloneTable().getInstance(
					grammar.getSchema(refBlock.getTypeConstraint().getType()));
			SchemaInstance targetInstance = grammarTables.getSchemaCloneTable().getInstance(
					grammar.getSchema(newMPoleType.getType()));

			for (Constraint c : refBlock.getConstraints()) {
				if (c.getSource().equals(refCxn.getName()) || !parentsOfGeneralCxn.contains(c.getSource())) {
					if (c.getOperator().equals(ECGConstants.ASSIGN)) {
						SlotChain argToUse = liftChain(c.getArguments().get(0), refInstance, targetInstance,
								newConstituentTypes);
						if (argToUse != null) {
							refConstraints.add(new Constraint(ECGConstants.ASSIGN, argToUse, c.getValue()));
						}
					}
					else {
						List<SlotChain> args = c.getArguments();
						List<SlotChain> newArgs = new ArrayList<SlotChain>();
						for (SlotChain arg : args) {
							newArgs.add(liftChain(arg, refInstance, targetInstance, newConstituentTypes));
						}
						if (!newArgs.contains(null)) {
							refConstraints.add(new Constraint(ECGConstants.IDENTIFY, newArgs));
						}
					}
				}
			}
		}
		else { // ontology type, or empty meaning pole. Eitherway all constraints are fair game.
			for (Constraint c : refBlock.getConstraints()) {
				refConstraints.add(c.clone());
			}
		}
		return refConstraints;
	}

	protected SlotChain liftChain(SlotChain arg, SchemaInstance refInstance, SchemaInstance targetInstance,
			Map<Role, String> newConstituentTypes) {

		Role firstRole = arg.getChain().get(0);
		if (firstRole.equals(mPole)) {
			SlotChain oldSchemaChain = new SlotChain().setChain(arg.getChain().subList(1, arg.getChain().size()));
			boolean hasSlot = targetInstance.hasSlot(targetInstance.getMainRoot(), oldSchemaChain);
			if (hasSlot) {
				return arg.clone();
			}
			else {
				List<SlotChain> otherPossibleChainsToUse = refInstance.getAllCoindexedSlotChainsBySlot(refInstance
						.getSlot(oldSchemaChain));
				for (SlotChain possibleChain : otherPossibleChainsToUse) {
					SlotChain newSchemaChain = new SlotChain().setChain(possibleChain.getChain().subList(1,
							possibleChain.getChain().size()));
					if (targetInstance.hasSlot(targetInstance.getMainRoot(), newSchemaChain)) {
						return new ECGSlotChain(ECGConstants.SELF + "." + ECGConstants.MEANING_POLE + "."
								+ newSchemaChain.toString());
					}
				}
				return null;
			}
		}
		else if (newConstituentTypes != null && newConstituentTypes.containsKey(firstRole)
				&& arg.getChain().get(1).equals(mPole)) {
			// if a constraint goes through a constituent that has been generalized to a new type,
			// try to lift this chain using the new meaning pole of the new constitutent type.
			TypeConstraint newRefTypeConstraint = grammar.getConstruction(firstRole.getTypeConstraint().getType())
					.getMeaningBlock().getTypeConstraint();
			if (newRefTypeConstraint.getTypeSystem().getName().equals(ECGConstants.SCHEMA)) {
				String newRefType = newRefTypeConstraint.getType();
				SchemaInstance newRef = grammarTables.getSchemaCloneTable().getInstance(grammar.getSchema(newRefType));
				String newTargetType = getUpdatedVersion(newConstituentTypes.get(firstRole)).getMeaningBlock()
						.getTypeConstraint().getType();
				SchemaInstance newTarget = grammarTables.getSchemaCloneTable()
						.getInstance(grammar.getSchema(newTargetType));

				// convert this temporarily to a self.m chain and recurse. Then have to convert back.
				SlotChain tmpChain = new ECGSlotChain(ECGConstants.SELF + "." + arg.subChain(1).toString());
				tmpChain = liftChain(tmpChain, newRef, newTarget, null);
				SlotChain newChain = new ECGSlotChain(firstRole.getName() + "." + tmpChain.subChain(0).toString());
				return newChain;
			}
			else {
				return arg.clone();
			}
		}
		else { // could be an evoke; will be dealt with later
			return arg.clone();
		}
	}

	protected Pair<List<Map<Role, Role>>, Integer> findSharedEvokes(Set<String> parentsOfGeneralCxn) {

		Set<Role> evokes0 = findGeneralizableEvokes(sourceCxn, parentsOfGeneralCxn);
		Set<Role> evokes1 = findGeneralizableEvokes(targetCxn, parentsOfGeneralCxn);
		if (evokes0.size() > 0 && evokes1.size() > 0) {
			int differenceInSize = Math.abs(evokes0.size() - evokes1.size());
			if (evokes0.size() <= evokes1.size()) {
				List<Map<Role, Role>> evokesMap = LearnerUtilities.mapConstituents(evokes0, evokes1,
						new EqualsMappingFunction());
				return new Pair<List<Map<Role, Role>>, Integer>(evokesMap, differenceInSize);
			}
			else {
				List<Map<Role, Role>> evokesMap = LearnerUtilities.mapConstituents(evokes1, evokes0,
						new EqualsMappingFunction());
				return new Pair<List<Map<Role, Role>>, Integer>(LearnerUtilities.reverseMappings(evokesMap),
						differenceInSize);
			}
		}
		return new Pair<List<Map<Role, Role>>, Integer>(new ArrayList<Map<Role, Role>>(), 0);
	}

	/**
	 * The parents of the new general cxn has to be passed down here because constraints would have been lifted up from
	 * the subcases into other general constructions. This new generalization would want to take inherited constraints
	 * into account, except for those that are inherited from the supertype of the new general cxn.
	 */
	protected Set<Role> findGeneralizableEvokes(Construction cxn, Set<String> parentsOfGeneralCxn) {
		Set<Role> evokes = new HashSet<Role>();
		for (Role r : cxn.getMeaningBlock().getEvokedElements()) {
			if (r.getSource().equals(cxn.getName()) || !parentsOfGeneralCxn.contains(r.getSource())) {
				evokes.add(r);
			}
		}
		return evokes;
	}

	protected TypeConstraint findMeaningPoleGeneralization() {

		Set<String> mPoleTypeNames = new HashSet<String>();
		TypeSystem<?> mPoleTS = null;
		boolean isAllNull = false;

		List<Construction> cxns = new ArrayList<Construction>();
		cxns.add(sourceCxn);
		cxns.add(targetCxn);

		for (Construction cxn : cxns) {
			TypeConstraint mPole = cxn.getMeaningBlock().getTypeConstraint();
			if (mPole == null) { // I guess the learner will eventually come across one with no meaning pole (e.g. function
										// morphemes)
				isAllNull = true;
			}
			else if (isAllNull == true) { // another cxn would have been null for this to have been set true.
				throw new LearnerException(
						"Trying to generalize over a mix of null and non-null meaning pole constructions");
			}
			else {
				mPoleTypeNames.add(mPole.getType());
				if (mPoleTS == null) {
					mPoleTS = mPole.getTypeSystem();
				}
				else if (mPoleTS != mPole.getTypeSystem()) {
					throw new LearnerException(
							"Different meaning pole type systems across the constructions to generalize over : "
									+ this.toString());
				}
			}
		}
		if (isAllNull)
			return null;

		TypeConstraint newMPoleType = null;
		try {
			String newMPoleTypeName = mPoleTS.bestCommonSupertype(mPoleTypeNames, false);
			newMPoleType = mPoleTS.getCanonicalTypeConstraint(newMPoleTypeName);
		}
		catch (TypeSystemException tse) {
			throw new LearnerException("TypeSystemException while trying to find meaning pole generalization for : "
					+ this.toString(), tse);
		}

		return newMPoleType;
	}

	protected TypeConstraint findBestEventTypeGeneralization(Collection<Construction> cxns) {
		Set<String> eventTypeNames = new HashSet<String>();
		TypeSystem<?> eventTS = null;

		for (Construction cxn : cxns) {
			TypeConstraint eventType = LearnerUtilities.findEventTypeRestriction(cxn, grammarTables);
			eventTypeNames.add(eventType.getType());
			if (eventTS == null) {
				eventTS = eventType.getTypeSystem();
			}
			else if (eventTS != eventType.getTypeSystem()) {
				throw new LearnerException(
						"Different eventType type systems across the constructions to generalize over : " + this.toString());
			}
		}
		TypeConstraint newMPoleType = null;
		try {
			String newMPoleTypeName = eventTS.bestCommonSupertype(eventTypeNames, false);
			newMPoleType = eventTS.getCanonicalTypeConstraint(newMPoleTypeName);
		}
		catch (TypeSystemException tse) {
			throw new LearnerException("TypeSystemException while trying to find event type generalization for : "
					+ this.toString(), tse);
		}

		return newMPoleType;
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		grammarTables = learnerGrammar.getGrammarTables();
		cxnTypeSystem = grammar.getCxnTypeSystem();
	}

	public TypeConstraint getSourceType() {
		return sourceType;
	}

	public TypeConstraint getTargetType() {
		return targetType;
	}

	public List<TypeConstraint> getGeneralizedOver() {
		List<TypeConstraint> generalized = new ArrayList<TypeConstraint>();
		generalized.add(sourceType);
		generalized.add(targetType);
		return generalized;
	}

	public void setSourceAndTarget(TypeConstraint sourceType, TypeConstraint targetType) {
		this.sourceType = sourceType;
		sourceCxn = grammar.getConstruction(sourceType.getType());
		this.targetType = targetType;
		targetCxn = grammar.getConstruction(targetType.getType());
	}

	public MapSet<String, String> getCategoriesToMerge() {
		return potentialCategoriesToMerge;
	}

	public String getNewCxnName() {
		return newCxnName;
	}

	public Map<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>> getFinalRoleMapping() {
		return finalRoleMapping;
	}

	private Construction getUpdatedVersion(String cxnName) {
		for (Construction c : additionsToGrammar) {
			if (c.getName().equals(cxnName)) {
				return c;
			}
		}

		Construction c = grammar.getConstruction(cxnName);
		if (substitutionToGrammar.containsKey(c)) {
			return substitutionToGrammar.get(c);
		}
		else {
			return c;
		}
	}

	public boolean isViable() {
		return isViable;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(sourceType.getType()).append(", ").append(targetType.getType());
		return sb.toString();
	}

}