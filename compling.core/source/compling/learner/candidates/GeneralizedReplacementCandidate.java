// =============================================================================
// File        : GeneralizedReplacementCandidate.java
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

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Block;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.ECGLearnerEngine;
import compling.learner.LearnerGrammar;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.grammartables.ConstructionCloneTable;
import compling.learner.util.GrammarModUtilities;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.Analysis;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class GeneralizedReplacementCandidate extends GeneralizationCandidate {

	private static final long serialVersionUID = -3968773057386434564L;

	int numRelaxations = 0;
	TypeConstraint a, b;

	Map<Role, Role> originalRoleMapping = new LinkedHashMap<Role, Role>();

	private static Logger logger = Logger.getLogger(GeneralizedReplacementCandidate.class.getName());

	public GeneralizedReplacementCandidate(LearnerGrammar learnerGrammar, TypeConstraint a, TypeConstraint b,
			List<Role> constrainedRoles) {
		super();
		this.a = a;
		this.b = b;
		setGrammar(learnerGrammar);
	}

	public void setConstituentMapping(TypeConstraint sourceType, Map<Role, Role> constituentMap) {
		setSourceAndTarget(sourceType, sourceType == a ? b : a);
		originalRoleMapping.putAll(constituentMap);
	}

	protected void createNewConstructions(Set<String> parents) throws TypeSystemException {

		// FIXME: this has to make sure that no more than maxRelationsAllowed constraints are dropped or relaxed

		Map<Role, String> newConstituentTypes = new HashMap<Role, String>();
		Map<Role, GeneralizationCandidate> triggeredGeneralizations = new HashMap<Role, GeneralizationCandidate>();

		// finding the sets of constituents/types to generalize over
		for (Role r : originalRoleMapping.keySet()) {
			TypeConstraint thisType = r.getTypeConstraint();
			TypeConstraint thatType = originalRoleMapping.get(r).getTypeConstraint();
			if (thisType != thatType) {
				String a = cxnTypeSystem.getInternedString(thisType.getType());
				String b = cxnTypeSystem.getInternedString(thatType.getType());
				if (cxnTypeSystem.subtype(a, b)) {
					// just use b
					newConstituentTypes.put(r, thatType.getType());
				}
				else if (cxnTypeSystem.subtype(b, a)) {
					// just use a
					newConstituentTypes.put(r, thisType.getType());
				}
				else {
					// a and b are different, but have they been previously generalized over?
					String existingGen = haveExistingGeneralization(a, b);
					if (existingGen != null) {
						newConstituentTypes.put(r, existingGen);
					}
					else {
						// generalize a and b
						GeneralizationCandidate triggered = GeneralizationFinder.generatePairwiseCandidate(learnerGrammar,
								r.getTypeConstraint(), thatType, new ArrayList<Role>(), false);
						if (triggered != null) {
							triggeredGeneralizations.put(r, triggered);
						}
						else {
							logger.finer("Replacement generalization between " + a + " and " + b
									+ " is not viable because a generalization cannot be found across constituents "
									+ r.getName() + " and " + originalRoleMapping.get(r).getName());
							isViable = false;
							return;
						}
					}
				}
			}
		}

		if (triggeredGeneralizations.isEmpty()) {
			// this is weird. If no generalizations are triggered, then one construction is most likely a generalization of
			// the other.
			logger.info("No recursive generalizations are triggered. Presumably these are either two constructions "
					+ "with same constituents and differing constraints, or with differing constituents that have already been generalized over");

			// in this case just get rid of the subcases.
			removalFromGrammar.add(sourceCxn);
			removalFromGrammar.add(targetCxn);
		}

		for (Role r : triggeredGeneralizations.keySet()) {
			GeneralizationCandidate triggered = triggeredGeneralizations.get(r);
			triggered.createNewConstructions();
			if (!triggered.isViable()) { // FUTURE: should the generalization be abandoned here or should a backup
													// categorization operation be carried out?
				logger.finer("Replacement generalization between " + a + " and " + b
						+ " is not viable because triggered generalization " + triggered.toString() + " is not viable");
				isViable = false;
				return;
			}
			GrammarChanges constituentChanges = triggered.getChanges();
			additionsToGrammar.addAll(constituentChanges.cxnsToAdd);
			substitutionToGrammar.putAll(constituentChanges.cxnsToReplace);
			newConstituentTypes.put(r, triggered.getNewCxnName());
			finalRoleMapping.putAll(triggered.getFinalRoleMapping());
			potentialCategoriesToMerge.putAll(triggered.getCategoriesToMerge());
		}

		// now have to make a general version of the source construction that uses the new categories
		// this will be a concrete construction, however.
		Block mBlock = null;
		if (triggeredGeneralizations.isEmpty()) {
			mBlock = sourceCxn.getMeaningBlock().clone();
		}
		else {
			Pair<Block, Map<Role, Role>> generalizedMBlock = findMeaningBlockGeneralization(newConstituentTypes, parents);
			if (generalizedMBlock == null) {
				logger.finer("Replacement generalization between " + a + " and " + b
						+ " is not viable due to problems in creating the new meaning block");
				isViable = false;
				return;
			}
			mBlock = generalizedMBlock.getFirst();
		}

		Construction newConcreteCxn = createConcreteConstruction(mBlock, newConstituentTypes);
		GrammarModUtilities.reannotateBlock(mBlock, newCxnName);
		additionsToGrammar.add(newConcreteCxn);
		isViable = true;
	}

	private String haveExistingGeneralization(String a, String b) {
		// if there isn't already a category containing a and b, see if there are generalizations over them.
		Set<String> children = new HashSet<String>();
		children.add(a);
		children.add(b);
		try {
			List<String> superTypes = cxnTypeSystem.allBestCommonSupertypes(children);
			superTypes.remove(ChildesLocalizer.MORPHEME);
			superTypes.remove(ChildesLocalizer.WORD);
			superTypes.remove(ChildesLocalizer.PHRASE);
			superTypes.remove(ChildesLocalizer.CLAUSE);
			if (superTypes.size() == 1) {
				return superTypes.iterator().next();
			}
			else if (superTypes.size() > 1) {
				// anyone of the remaining type is fine. FUTURE: Try using number of children as a tie-breaker?
				return superTypes.get(0);
			}
		}
		catch (TypeSystemException tse) {
			// no common supertype? that's weird
			logger.warning("TypeSystemException when trying to find existing generalizations");
		}

		return learnerGrammar.getGeneralizationHistory().mostAggressiveGeneralization(a, b);
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

		// For generalized replacement, the shared constraints must be calculated both ways in order to keep track of
		// the total number of constraints from each of the subcases that got dropped / relaxed.

		List<Constraint> sharedConstraints = null;
		Pair<List<Map<Role, Role>>, Integer> sharedEvokes = findSharedEvokes(parentsOfGeneralCxn);
		numRelaxations += sharedEvokes.getSecond();

		Map<Role, Role> bestEvokesMap = null;

		// Map the meaning block of the source cxn onto the new meaning type if it's a schema type.
		// This is the set of constraints that are expressable given the new meaning pole type; the roles they touch
		// ought to be inherited by the other constructions except the ones that goes through the evokes need to be mapped

		List<Constraint> liftedConstraints;
		if (newConstituentTypes.isEmpty()) {
			liftedConstraints = new ArrayList<Constraint>();
			for (Constraint c : sourceCxn.getMeaningBlock().getConstraints()) {
				liftedConstraints.add(c.clone());
			}
		}
		else {
			liftedConstraints = liftConstraints(newMPoleType, sourceCxn, newConstituentTypes, parentsOfGeneralCxn);
		}
		numRelaxations += sourceCxn.getMeaningBlock().getConstraints().size() - liftedConstraints.size();
		if (numRelaxations > GeneralizationFinder.maxRelationsAllowed) {
			return null;
		}

		ConstructionCloneTable cct = learnerGrammar.getGrammarTables().getConstructionCloneTable();

		if (sharedEvokes.getFirst().isEmpty()) {
			Pair<List<Constraint>, Integer> shared = findSharedConstraint(liftedConstraints, null, cct);
			if (shared == null)
				return null;
			sharedConstraints = shared.getFirst();
			numRelaxations += shared.getSecond();
		}
		else {
			// choose the evoke mapping that yields the greatest number of shared constraints
			Map<Map<Role, Role>, Pair<List<Constraint>, Integer>> sharedConstraintsPerEvokeMapping = new HashMap<Map<Role, Role>, Pair<List<Constraint>, Integer>>();
			int maxShared = 0;
			for (Map<Role, Role> map : sharedEvokes.getFirst()) {
				Pair<List<Constraint>, Integer> shared = findSharedConstraint(liftedConstraints, map, cct);
				if (shared != null) {
					sharedConstraintsPerEvokeMapping.put(map, shared);
					if (shared.getFirst().size() > maxShared) {
						maxShared = shared.getFirst().size();
						bestEvokesMap = map;
					}
				}
			}
			if (bestEvokesMap == null)
				return null;

			sharedConstraints = sharedConstraintsPerEvokeMapping.get(bestEvokesMap).getFirst();
			numRelaxations += sharedConstraintsPerEvokeMapping.get(bestEvokesMap).getSecond();
			Set<Role> newEvokedRoles = new HashSet<Role>();
			for (Role r : bestEvokesMap.keySet()) {
				newEvokedRoles.add(r.clone());
			}
			meaningBlock.setEvokedElements(newEvokedRoles);
		}
		if (numRelaxations > GeneralizationFinder.maxRelationsAllowed) {
			return null;
		}

		// Finally, in both cases (EventDescriptor or not), populate the meaning block with these shared constraints
		meaningBlock.setConstraints(new LinkedHashSet<Constraint>(sharedConstraints));
		return new Pair<Block, Map<Role, Role>>(meaningBlock, bestEvokesMap);
	}

	private Pair<List<Constraint>, Integer> findSharedConstraint(List<Constraint> lifted, Map<Role, Role> evokesMap,
			ConstructionCloneTable cct) {

		int numRelaxed = 0;
		MapSet<Constraint, Set<String>> relaxTo = new MapSet<Constraint, Set<String>>();
		List<Constraint> sharedConstraints = new ArrayList<Constraint>(lifted);

		Map<Role, Role> completeMap = new LinkedHashMap<Role, Role>();
		for (Role r : originalRoleMapping.keySet()) {
			completeMap.put(r, originalRoleMapping.get(r));
		}
		if (evokesMap != null) {
			completeMap.putAll(evokesMap);
		}

		for (Constraint constraint : lifted) {
			Analysis semspec = cct.getInstance(targetCxn).clone();

			Constraint mappedConstraint = LearnerUtilities.mapConstraint(constraint, completeMap);
			boolean success = LearnerUtilities.isInAnalysis(mappedConstraint, semspec, grammar, false);

			if (!success) {
				numRelaxed++;
				if (constraint.getOperator().equals(ECGConstants.ASSIGN) && GeneralizationFinder.maxRelationsAllowed > 0) {
					Set<String> relaxable = findRelaxedContextualConstraint(grammar, constraint, semspec);
					if (relaxable == null) { // if no relaxation is possible, drop
						sharedConstraints.remove(constraint);
					}
					else { // otherwise wait till the end to get the intersection of the pool of relaxations
						relaxTo.put(constraint, relaxable);
					}
				}
				else {
					// this should raise a red flag -- some unification constraints conflict
					return null;
				}
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

		return new Pair<List<Constraint>, Integer>(sharedConstraints, numRelaxed);
	}

	protected Construction createConcreteConstruction(Block meaningBlock, Map<Role, String> newCatNames) {
		newCxnName = String.format("%s%2$03d", ECGLearnerEngine.GEN_CXN_PREFIX, ECGLearnerEngine.getCounter());
		List<Role> sourceConstituents = new ArrayList<Role>(grammar.getConstruction(sourceType.getType())
				.getConstructionalBlock().getElements());

		Map<Role, Pair<String, TypeConstraint>> replacements = new HashMap<Role, Pair<String, TypeConstraint>>();

		for (Role sourceRole : newCatNames.keySet()) {
			String constituentTypeName = newCatNames.get(sourceRole);
			TypeConstraint constituentType = new TypeConstraint(constituentTypeName, cxnTypeSystem);
			String constituentName = String.format("%s%2$01d", constituentTypeName.substring(0, 1).toLowerCase(),
					sourceConstituents.indexOf(sourceRole));
			Role collidingRole = sourceCxn.getConstructionalBlock().getRole(constituentName);
			char suffix = 'a';
			while (collidingRole != null && collidingRole != sourceRole) {
				// must rename
				constituentName = String.format("%s%2$01d%3$c", constituentTypeName.substring(0, 1).toLowerCase(),
						sourceConstituents.indexOf(sourceRole), suffix);
				collidingRole = sourceCxn.getConstructionalBlock().getRole(constituentName);
				suffix++;
			}
			Pair<String, TypeConstraint> newConstituent = new Pair<String, TypeConstraint>(constituentName,
					constituentType);
			replacements.put(sourceRole, newConstituent);
		}

		Construction newCxn = GrammarModUtilities.modifyConstituents(grammar, sourceCxn, newCxnName, replacements,
				meaningBlock);

		// create the final mapping between the new roles in the concrete construction and those generalized over (for the
		// purpose of aggregating counts later)
		Block newCxnBlock = newCxn.getConstructionalBlock();
		for (Role sourceRole : originalRoleMapping.keySet()) {
			Role genRole = null;
			if (replacements.containsKey(sourceRole)) {
				// there is a generalization across this constituent
				String newConstituentName = replacements.get(sourceRole).getFirst();
				genRole = newCxnBlock.getRole(newConstituentName);
			}
			else {
				// no generalization, so the role name is the same as found in the most general amongst the generalized
				genRole = newCxnBlock.getRole(sourceRole.getName());
			}

			MapMap<Role, TypeConstraint, Role> thisFinalRoleMapping = new MapMap<Role, TypeConstraint, Role>();
			thisFinalRoleMapping.put(genRole, sourceType, sourceRole);
			thisFinalRoleMapping.put(genRole, targetType, originalRoleMapping.get(sourceRole));
			finalRoleMapping.put(this, thisFinalRoleMapping);
		}
		return newCxn;
	}

}
