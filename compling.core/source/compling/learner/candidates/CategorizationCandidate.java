// =============================================================================
// File        : CategorizationCandidate.java
// Author      : emok
// Change Log  : Created on May 11, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.ECGLearnerEngine;
import compling.learner.LearnerGrammar;
import compling.learner.grammartables.ConstructionCloneTable;
import compling.learner.util.GrammarModUtilities;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.Analysis;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================
/***
 * Forming a new category is quite similar to making a generalized replacement constructions, except that the new
 * abstract category is not required to contain all the constraints expressed in the subcases, since the subcases are
 * going to inherit from this new category.
 */

public class CategorizationCandidate extends GeneralizationCandidate {

	private static final long serialVersionUID = -8243914634171743017L;

	private static Logger logger = Logger.getLogger(CategorizationCandidate.class.getName());

	public CategorizationCandidate(LearnerGrammar learnerGrammar, TypeConstraint a, TypeConstraint b) {
		super();
		setGrammar(learnerGrammar);
		setSourceAndTarget(a, b);
	}

	protected void createNewConstructions(Set<String> parents) {
		// just make a new abstract category with the constructions as members. Lift the meaning constraints but not the
		// form ones.
		// In this case the lifted constraints are deleted from the concrete ones.

		newCxnName = String.format("%s%2$03d", ECGLearnerEngine.CAT_CXN_PREFIX, ECGLearnerEngine.getCounter());
		Pair<Block, Map<Role, Role>> newMBlock = findMeaningBlockGeneralization(null, parents);
		Construction newAbstractCxn = createAbstractConstruction(newCxnName, parents, newMBlock.getFirst());
		additionsToGrammar.add(newAbstractCxn);
		Map<Role, Role> reverseEvokesMap = newMBlock.getSecond() == null ? null : LearnerUtilities
				.reverseMappings(newMBlock.getSecond());

		List<Construction> cxns = new ArrayList<Construction>();
		cxns.add(sourceCxn);
		cxns.add(targetCxn);

		for (Construction oldSubcaseCxn : cxns) {
			Set<String> newParents = new HashSet<String>(oldSubcaseCxn.getParents());
			newParents.add(newCxnName);
			Construction newSubcaseCxn = GrammarModUtilities.modifyParents(grammar, oldSubcaseCxn, newParents);

			Map<Construction, Construction> changesDueToRenaming = cleanupLiftedConstraints(newSubcaseCxn, newAbstractCxn,
					oldSubcaseCxn.equals(sourceCxn) ? newMBlock.getSecond() : reverseEvokesMap);
			substitutionToGrammar.putAll(changesDueToRenaming);
			substitutionToGrammar.put(oldSubcaseCxn, newSubcaseCxn);

			if (!oldSubcaseCxn.isConcrete()) {
				potentialCategoriesToMerge.put(newCxnName, oldSubcaseCxn.getName());
			}
		}
		isViable = true;
	}

	private Pair<Block, Map<Role, Role>> findMeaningBlockGeneralization(Map<Role, String> newConstituentTypes,
			Set<String> parentsOfGeneralCxn) {
		Block meaningBlock;
		TypeConstraint newMPoleType = findMeaningPoleGeneralization();
		if (newMPoleType == null) {
			meaningBlock = grammar.new Block(ECGConstants.MEANING, ECGConstants.UNTYPED);
		}
		else {
			meaningBlock = grammar.new Block(ECGConstants.MEANING, newMPoleType.getType());
			meaningBlock.setBlockTypeTypeSystem(newMPoleType.getTypeSystem());
		}

		List<Constraint> sharedConstraints = null;
		Pair<List<Map<Role, Role>>, Integer> sharedEvokes = findSharedEvokes(parentsOfGeneralCxn);
		Map<Role, Role> bestEvokesMap = null;

		// Map the meaning block of the source cxn onto the new meaning type if it's a schema type.
		// This is the set of constraints that are expressable given the new meaning pole type; the roles they touch
		// ought to be inherited by the other constructions except the ones that goes through the evokes need to be mapped
		List<Constraint> liftedConstraints = liftConstraints(newMPoleType, sourceCxn, newConstituentTypes,
				parentsOfGeneralCxn);
		ConstructionCloneTable cct = learnerGrammar.getGrammarTables().getConstructionCloneTable();

		if (sharedEvokes.getFirst().isEmpty()) {
			sharedConstraints = findSharedConstraint(liftedConstraints, null, cct);
		}
		else {
			// choose the evoke mapping that yields the greatest number of shared constraints
			Map<Map<Role, Role>, List<Constraint>> sharedConstraintsPerEvokeMapping = new HashMap<Map<Role, Role>, List<Constraint>>();
			int maxShared = 0;
			for (Map<Role, Role> map : sharedEvokes.getFirst()) {
				List<Constraint> shared = findSharedConstraint(liftedConstraints, map, cct);
				sharedConstraintsPerEvokeMapping.put(map, shared);
				if (shared.size() > maxShared) {
					maxShared = shared.size();
					bestEvokesMap = map;
				}
			}
			sharedConstraints = sharedConstraintsPerEvokeMapping.get(bestEvokesMap);
			Set<Role> newEvokedRoles = new HashSet<Role>();
			for (Role r : bestEvokesMap.keySet()) {
				newEvokedRoles.add(r.clone());
			}
			meaningBlock.setEvokedElements(newEvokedRoles);
		}

		// Finally, in both cases (EventDescriptor or not), populate the meaning block with these shared constraints
		meaningBlock.setConstraints(new LinkedHashSet<Constraint>(sharedConstraints));
		return new Pair<Block, Map<Role, Role>>(meaningBlock, bestEvokesMap);
	}

	private List<Constraint> findSharedConstraint(List<Constraint> lifted, Map<Role, Role> evokesMap,
			ConstructionCloneTable cct) {
		MapSet<Constraint, Set<String>> relaxTo = new MapSet<Constraint, Set<String>>();
		List<Constraint> sharedConstraints = new ArrayList<Constraint>(lifted);

		// NOTE: the evokesMap makes the assumption that there are only two cxns being generalized over.

		Map<Role, Role> completeMap = null;
		if (evokesMap != null) {
			completeMap = new LinkedHashMap<Role, Role>();
			completeMap.putAll(evokesMap);
		}

		for (Constraint constraint : lifted) {
			Analysis semspec = cct.getInstance(targetCxn).clone();

			int originalSize = semspec.getFeatureStructure().getSlots().size();
			boolean success = completeMap == null ? semspec.addConstraint(constraint, "") : semspec.addConstraint(
					LearnerUtilities.mapConstraint(constraint, completeMap), "");
			int newSize = semspec.getFeatureStructure().getSlots().size();

			if (!success && constraint.getOperator().equals(ECGConstants.ASSIGN)) {
				Set<String> relaxable = findRelaxedContextualConstraint(grammar, constraint, semspec);
				if (relaxable == null) { // if no relaxation is possible, then just drop this shared constraint
					sharedConstraints.remove(constraint);
				}
				else { // otherwise wait till the end to get the intersection of the pool of relaxations
					relaxTo.put(constraint, relaxable);
				}
			}
			else if (!success || newSize != originalSize) {
				sharedConstraints.remove(constraint);
			}
		}

		// now relax all those that can be relaxed. If no agreement can be reached on relaxation, drop.
		for (Constraint constraint : relaxTo.keySet()) {
			Iterator<Set<String>> iter = relaxTo.get(constraint).iterator();
			List<String> intersection = new ArrayList<String>(iter.next());
			while (iter.hasNext()) {
				intersection.retainAll(iter.next());
			}
			if (intersection.isEmpty()) {
				sharedConstraints.remove(constraint);
			}
			else {
				relaxConstraint(constraint, intersection.get(0));
			}
		}

		return sharedConstraints;
	}

	protected Construction createAbstractConstruction(String cxnName, Set<String> parents, Block meaningBlock) {

		Block constructionalBlock = grammar.new Block(ECGConstants.CONSTRUCTIONAL, ECGConstants.UNTYPED);
		GrammarModUtilities.reannotateBlock(constructionalBlock, cxnName);

		Block formBlock = grammar.new Block(ECGConstants.FORM, ECGConstants.UNTYPED);
		GrammarModUtilities.reannotateBlock(formBlock, cxnName);

		GrammarModUtilities.reannotateBlock(meaningBlock, cxnName);

		Construction newConstruction = grammar.new Construction(cxnName, ECGConstants.ABSTRACT, parents, formBlock,
				meaningBlock, constructionalBlock);

		newConstruction.setComplements(new HashSet<Role>());
		newConstruction.setOptionals(new HashSet<Role>());

		return newConstruction;
	}

	protected Map<Construction, Construction> cleanupLiftedConstraints(Construction subcaseCxn,
			Construction abstractCxn, Map<Role, Role> evokesMap) {

		Map<Construction, Construction> changesDueToRenaming = new LinkedHashMap<Construction, Construction>();

		// Remove constraints that have been refactored to the abstract construction.
		// HACK: Must do this first or removal doesn't work properly.
		Analysis absCxn = new Analysis(abstractCxn); // this is new and not yet committed to the grammar, so it can't be
																	// found in table
		// List<Constraint> toRemove = new ArrayList<Constraint>();
		for (Iterator<Constraint> iter = subcaseCxn.getMeaningBlock().getConstraints().iterator(); iter.hasNext();) {
			Constraint constraint = subcaseCxn.getName().equals(sourceCxn.getName()) || evokesMap == null ? iter.next()
					: LearnerUtilities.mapConstraint(iter.next(), evokesMap);
			if (LearnerUtilities.isInAnalysis(constraint, absCxn, grammar, true)) {
				iter.remove();
			}
		}

		// first make sure that the new local names for the evoked elements doesn't conflict with existing local roles
		// if conflict exists, rename the conflicting evoked elements
		if (!subcaseCxn.getName().equals(sourceCxn.getName())) {
			if (evokesMap != null) {
				for (Role mappedRole : evokesMap.keySet()) {
					String mappedTo = evokesMap.get(mappedRole).getName();
					if (mappedRole.getName().equals(mappedTo))
						continue;

					char suffix = 'a';
					while (subcaseCxn.getMeaningBlock().getRole(mappedTo) != null) {
						// the mappedTo local name (which is different from the original local name) already exists; there is
						// a role name collision
						mappedTo = evokesMap.get(mappedRole).getName().concat(String.valueOf(suffix));
						suffix++;
					}
					Role newRole = mappedRole.clone();
					newRole.setName(mappedTo);
					changesDueToRenaming.putAll(GrammarModUtilities.renameEvokedRole(subcaseCxn, mappedRole, newRole,
							learnerGrammar));
				}
			}
		}

		// also remove the lifted evokes
		if (evokesMap != null) {
			for (Role evoked : evokesMap.keySet()) {
				if (!subcaseCxn.getName().equals(sourceCxn.getName())
						&& !evoked.getName().equals(evokesMap.get(evoked).getName())) {
					changesDueToRenaming.putAll(GrammarModUtilities.renameEvokedRole(subcaseCxn, evoked,
							evokesMap.get(evoked), learnerGrammar));
				}
				subcaseCxn.getMeaningBlock().getEvokedElements().remove(evoked);
			}
		}

		return changesDueToRenaming;
	}
}
