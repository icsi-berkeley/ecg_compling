// =============================================================================
// File        : CategoryMerger.java
// Author      : emok
// Change Log  : Created on Apr 13, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.ECGLearnerEngine;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.grammartables.ConstituentCooccurenceTable;
import compling.learner.grammartables.GrammarTables;
import compling.learner.util.GrammarModUtilities;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

//=============================================================================

public class CategoryMerger {

	// This class makes one crucial assumption: the supertype is already assumed to be an
	// abstraction over the toBeMerged. CategoryExpander should have done all the necessary
	// lining up of roles. Whatever evoked item from the toBeMerged that doesn't
	// appear in the supertype are assumed to not be abstractable and are pushed back downwards.

	LearnerGrammar learnerGrammar;
	Grammar grammar;
	GrammarTables grammarTables;
	TypeSystem<Construction> cxnTypeSystem;

	String supertypeName;
	Set<String> categoriesToMerge;

	Iterator<String> iterator;

	Map<Construction, Construction> toReplace = new LinkedHashMap<Construction, Construction>();
	List<Construction> toPurge = new ArrayList<Construction>();

	static Logger logger = Logger.getLogger(CategoryMerger.class.getName());

	/***
	 * categoriesToMerge: MapSet<supertype, subtypesToBeMergedIntoSupertype>
	 */
	public CategoryMerger(LearnerGrammar learnerGrammar, String supertype, Set<String> toBeMergedIntoSupertype) {
		setGrammar(learnerGrammar);
		this.supertypeName = supertype;
		this.categoriesToMerge = toBeMergedIntoSupertype;
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		grammarTables = learnerGrammar.getGrammarTables();
		cxnTypeSystem = grammar.getCxnTypeSystem();
	}

	public GrammarChanges mergeCategories() {

		TypeConstraint supertype = cxnTypeSystem.getCanonicalTypeConstraint(supertypeName);
		Construction supertypeCxn = cxnTypeSystem.get(supertypeName);

		for (String cxnName : categoriesToMerge) {
			TypeConstraint cxnType = cxnTypeSystem.getCanonicalTypeConstraint(cxnName);
			Construction cxn = grammar.getConstruction(cxnName);
			fixupSubcasesOfMergedCategory(supertypeCxn, cxnName, cxnType, cxn);
			fixupUsersOfMergedCategory(supertype, cxnName, cxnType);
			toPurge.add(cxn);
		}
		return new GrammarChanges(null, toReplace, toPurge);
	}

	private void fixupUsersOfMergedCategory(TypeConstraint supertype, String cxnName, TypeConstraint cxnType) {
		ConstituentCooccurenceTable cooccurenceTable = grammarTables.getConstituentCooccurenceTable();
		cooccurenceTable.setQueryExpander(null);
		Collection<Set<TypeConstraint>> userGroups = cooccurenceTable.findCoveringTypes(cxnType).values();
		if (userGroups.size() > 1) {
			// this shouldn't happen -- there is no query expander so only one substitution is possible
			throw new LearnerException(
					"There shouldn't be more than one set of results when looking up constructions that use " + cxnName);
		}
		if (userGroups.size() < 1) {
			logger.warning("An orphaned construction encountered: " + cxnName);
			return;
		}
		Set<TypeConstraint> users = userGroups.iterator().next();

		// change the constituents of the cxns that use the old cxn
		for (TypeConstraint userType : users) {
			Construction user = grammar.getConstruction(userType.getType());
			if (toReplace.containsKey(user)) {
				user = toReplace.get(user);
			}

			Map<Role, Pair<String, TypeConstraint>> replacements = new HashMap<Role, Pair<String, TypeConstraint>>();

			List<Role> userConstituents = new ArrayList<Role>(user.getConstructionalBlock().getElements());
			for (Role r : userConstituents) {
				if (r.getTypeConstraint() == cxnType) {
					String constituentName = String.format("%s%2$01d", cxnName.substring(0, 1).toLowerCase(),
							userConstituents.indexOf(r));
					replacements.put(r, new Pair<String, TypeConstraint>(constituentName, supertype));
				}
			}
			Construction newUser = GrammarModUtilities.modifyConstituents(grammar, user, user.getName(), replacements,
					null);
			if (isAlreadySubsumed(newUser, grammar.getConstruction(supertypeName), toReplace)) {
				// the newUser is possibly already subsumed by a construction (e.g. if this is merging a bogus new category
				// created by a generalization)
				// if the newUser is subsumed, just delete the original user from the grammar
				toPurge.add(user);
			}
			else {
				toReplace.put(user, newUser);
			}
		}
	}

	private void fixupSubcasesOfMergedCategory(Construction supertypeCxn, String toBeMergedName,
			TypeConstraint toBeMergedType, Construction toBeMerged) {
		// Since the two categories to be merged aren't necessarily in any subcase relation,
		// for all constraints and evokes in toBeMerged that are not in the supertype, push back down.
		// I think this check is quite possibly redundant given the supertype abstraction assumption.

		List<Role> evokesToBePushedBackDown = new ArrayList<Role>();
		List<Constraint> constraintsToBePushedBackDown = new ArrayList<Constraint>();

		if (toBeMerged.getMeaningBlock() != null) {
			for (Role r : toBeMerged.getMeaningBlock().getEvokedElements()) {
				if (supertypeCxn.getMeaningBlock().getRole(r.getName()) == null) {
					evokesToBePushedBackDown.add(r);
				}
			}

			Analysis superTypeSemspec = grammarTables.getConstructionCloneTable().getInstance(
					grammar.getConstruction(supertypeName));
			for (Constraint c : toBeMerged.getMeaningBlock().getConstraints()) {
				if (!LearnerUtilities.isInAnalysis(c, superTypeSemspec, grammar, false)) {
					constraintsToBePushedBackDown.add(c);
				}
			}
		}

		for (Construction child : cxnTypeSystem.getChildren(toBeMerged)) {
			// I'm using modifyRoles here to essentially get a clone of the construction.
			Construction replacement = toReplace.containsKey(child) ? toReplace.get(child) : GrammarModUtilities
					.modifyRoles(grammar, child, child.getName(), new HashMap<Role, Role>(), null, false);

			// change the subtypes of the old cxn to use the new supertype.
			// watch out for multiple inheritance. Get the parents from the toReplace table if it's there.
			Set<String> newParents = new HashSet<String>(replacement.getParents());
			newParents.remove(toBeMergedName);
			newParents.add(supertypeName);
			replacement.setParents(newParents);

			// push unabstracted stuff back down
			if (!evokesToBePushedBackDown.isEmpty() || !constraintsToBePushedBackDown.isEmpty()) {
				for (Role r : evokesToBePushedBackDown) {
					replacement.getMeaningBlock().getEvokedElements().add(r.clone());
				}
				for (Constraint c : constraintsToBePushedBackDown) {
					replacement.getMeaningBlock().getConstraints().add(c.clone());
					// I wonder if this will work -- to clone or not to clone? cloning might make it a different type system
					// and break things
				}
				GrammarModUtilities.reannotateBlock(replacement.getMeaningBlock(), child.getName());
			}

			toReplace.put(child, replacement);
		}
	}

	protected boolean isAlreadySubsumed(Construction newUser, Construction mergedCategory,
			Map<Construction, Construction> toReplace) {
		List<Construction> constituents = new ArrayList<Construction>();
		constituents.add(mergedCategory);
		Set<Construction> cxnsToCheckAgainst = grammarTables.getConstituentCooccurenceTable().findTypeWithConstituents(
				constituents);
		cxnsToCheckAgainst.addAll(toReplace.values());
		Set<Construction> subsumbedBy = ECGLearnerEngine.getSubsuming(learnerGrammar, newUser, cxnsToCheckAgainst,
				grammarTables);
		return !subsumbedBy.isEmpty();
	}

}
