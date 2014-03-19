// =============================================================================
// File        : CategoryExpander.java
// Author      : emok
// Change Log  : Created on May 13, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerGrammar;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.grammartables.GrammarTables;
import compling.learner.grammartables.Substitution;
import compling.learner.learnertables.ConstructionalSubtypeTable;
import compling.learner.learnertables.NGram;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class CategoryExpander {

	// There are two kinds of category expansions that can happen:

	// 1. Merging two categories that share some common members (how many?)

	// 2. When a constructional category has the semantic correlate of a schema category (e.g. Toy), and there are enough
	// members of the
	// semantic category that are meanings of subcases of the constructional category (e.g. che1 - Car, qiu2 - Ball)
	// extend the constructional categories to all members (e.g. wa1 - Doll, xiong2 - Bear).

	// The problem is, when two categories are merged, some constraints that are lifted into the two categories
	// are going to differ. When they do, only the intersection of those will be put into the resulting category.
	// In this case, the ones that do not make it into the merged category will have to be put back into the subcases.

	LearnerGrammar learnerGrammar = null;
	Grammar grammar = null;
	GrammarTables grammarTables = null;
	TypeSystem<Construction> cxnTypeSystem = null;
	List<String> recentlyTouched = new ArrayList<String>();
	MapSet<String, String> knownCategoriesToMerge = new MapSet<String, String>();

	Iterator<String> recentlyTouchedIter;

	GrammarChanges changes = null;
	private static Logger logger = Logger.getLogger(CategoryExpander.class.getName());

	public CategoryExpander(LearnerGrammar learnerGrammar) {
		this(learnerGrammar, null);
	}

	public CategoryExpander(LearnerGrammar learnerGrammar, Collection<String> recentlyTouchedConstructions) {
		setGrammar(learnerGrammar);
		addRecentlyTouched(recentlyTouchedConstructions);
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		cxnTypeSystem = grammar.getCxnTypeSystem();
		grammarTables = learnerGrammar.getGrammarTables();
	}

	public void addRecentlyTouched(Collection<String> recentlyTouchedConstructions) {
		if (recentlyTouchedConstructions != null) {
			recentlyTouched.addAll(recentlyTouchedConstructions);
		}
		recentlyTouchedIter = recentlyTouched.iterator();
	}

	public boolean expandCategories(int approach) {
		if (recentlyTouchedIter.hasNext()) {
			String concreteType = recentlyTouchedIter.next();
			return agressiveExpansion(concreteType, approach);
		}
		return false;
	}

	protected boolean agressiveExpansion(String concreteType, int approach) {
		// There are still two ways to expand the categories that this concreteType belongs to.
		// The first is to merge its supertypes and the second is to extend each supertype to include semantically related
		// words.

		List<String> parents = new ArrayList<String>(grammar.getConstruction(concreteType).getParents());
		parents.removeAll(ChildesLocalizer.GIVEN_CATEGORIES);

		// first approach: merge supertypes

		boolean grammarModified = approach == 1 ? approach1(parents) : approach2(parents);
		return grammarModified;
	}

	protected boolean approach1(Collection<String> supertypesToMerge) {
		// While possible, just keep merging the supertypes.
		// easiest way to accomplish this using existing machinary is to use CategorizationCandidate to abtract over them
		// do this blindly for now. Later semantics and perhaps distributional properties should be checked.

		// FUTURE: the current implementation is insanely expensive -- grammars and tables are updated after every step to
		// preserve correctness.

		List<String> stack = new ArrayList<String>(supertypesToMerge);
		Set<Pair<String, String>> previousAttempts = new HashSet<Pair<String, String>>();

		// Example:
		// ni3 subcase of cat01, cat02, cat03, Morpheme -
		// generalize cat01, cat02 --> get cat04
		// merge cat01, cat02 into cat04
		// remainder in queue cat03 and cat04

		boolean grammarModified = false;

		while (!stack.isEmpty()) {
			String catA = stack.remove(0);
			String catB = null;
			for (String type : stack) {
				Pair<String, String> attempt = catA.compareTo(type) < 0 ? new Pair<String, String>(catA, type)
						: new Pair<String, String>(type, catA);
				if (areGoodCandidates(attempt, previousAttempts)) {
					catB = type;
					previousAttempts.add(attempt);
					break;
				}
			}
			if (catB == null) {
				// nothing can be merged with catA, so remove catA from stack
				stack.remove(catA);
			}
			else {
				stack.remove(catB);
			}

			// caching the old tables since we are getting rid of some constructions and we need the old numbers
			ConstructionalSubtypeTable oldCxnTable = learnerGrammar.getConstructionalSubtypeTable();
			NGram oldNGram = learnerGrammar.getNGram();
			Set<String> toMerge = new LinkedHashSet<String>();
			try {
				toMerge.addAll(cxnTypeSystem.getAllSubtypes(catA));
				toMerge.addAll(cxnTypeSystem.getAllSubtypes(catB));
				toMerge.remove(catA);
				toMerge.remove(catB);
			}
			catch (TypeSystemException tse) {
				logger.warning("TypeSystemException getting subtypes of abstract categories " + tse.getLocalizedMessage());
				continue;
			}

			TypeConstraint typeA = cxnTypeSystem.getCanonicalTypeConstraint(catA);
			TypeConstraint typeB = cxnTypeSystem.getCanonicalTypeConstraint(catB);
			Pair<Boolean, String> modified = mergeIntoOneCategory(catA, catB, typeA, typeB);
			if (!modified.getFirst()) {
				// this didn't work. Put both back onto the stack and try some other combinations
				stack.add(0, catB);
				stack.add(0, catA);
			}
			else {
				grammarModified = true;
				learnerGrammar.updateTablesAfterCategoryMerge(modified.getSecond(), toMerge, oldCxnTable, oldNGram, true);
			}
		}

		return grammarModified;
	}

	private Pair<Boolean, String> mergeIntoOneCategory(String catA, String catB, TypeConstraint typeA,
			TypeConstraint typeB) {
		// now generalize typeA and typeB
		CategorizationCandidate candidate = new CategorizationCandidate(learnerGrammar, typeA, typeB);
		candidate.createNewConstructions();
		GrammarChanges abstractionChanges = candidate.getChanges();
		boolean abstracted = learnerGrammar.modifyGrammar(abstractionChanges);
		if (!abstracted) {
			return new Pair<Boolean, String>(false, null);
		}
		else {
			setGrammar(learnerGrammar);
			String newCatName = candidate.getNewCxnName();
			boolean successfullyMerged = true;
			for (String supertype : candidate.getCategoriesToMerge().keySet()) {
				// now that a new cat is created above typeA and typeB, merge both into the new cat
				Set<String> toMerge = new HashSet<String>();
				toMerge.add(catA);
				toMerge.add(catB);
				CategoryMerger merger = new CategoryMerger(learnerGrammar, supertype, candidate.getCategoriesToMerge().get(
						supertype));
				GrammarChanges mergeChanges = merger.mergeCategories();
				boolean updated = learnerGrammar.modifyGrammar(mergeChanges);
				if (updated) {
					setGrammar(learnerGrammar);
				}
				successfullyMerged &= updated;
			}
			if (!successfullyMerged) {
				return new Pair<Boolean, String>(false, null);
			}
			else {
				setGrammar(learnerGrammar);
				return new Pair<Boolean, String>(true, newCatName);
			}
		}
	}

	protected boolean areGoodCandidates(Pair<String, String> currentAttempt, Set<Pair<String, String>> previousAttempts) {

		if (previousAttempts.contains(currentAttempt))
			return false;

		try {
			Set<String> aDescendent = cxnTypeSystem.getAllSubtypes(currentAttempt.getFirst());
			aDescendent.remove(currentAttempt.getFirst());
			Set<String> bDescendent = cxnTypeSystem.getAllSubtypes(currentAttempt.getSecond());
			bDescendent.remove(currentAttempt.getSecond());

			Set<String> sharedDescendent = new HashSet<String>(aDescendent);
			sharedDescendent.retainAll(bDescendent);

			if (sharedDescendent.size() >= (aDescendent.size() / 2.0)
					&& sharedDescendent.size() >= (bDescendent.size() / 2.0)) {
				return true;
			}
		}
		catch (TypeSystemException tse) {
			logger.warning("TypeSystemException encountered when assessing whether " + currentAttempt
					+ " are good for merging.");
		}
		return false;
	}

	protected boolean approach2(Collection<String> superTypes) {
		// This approach extends the categories to include all other semantically related words.

		boolean modified = false;
		for (String supertype : superTypes) {
			// for each supertype, find the meaning pole
			Set<TypeConstraint> membersToExtendTo = findMembersToExtendTo(supertype);

			String currentSupertype = supertype;
			if (!membersToExtendTo.isEmpty()) {
				for (TypeConstraint newMember : membersToExtendTo) {
					Pair<Boolean, String> newCat = mergeIntoOneCategory(currentSupertype, newMember.getType(),
							cxnTypeSystem.getCanonicalTypeConstraint(currentSupertype), newMember);
					if (newCat.getFirst()) {
						modified = true;
						currentSupertype = newCat.getSecond();
					}
				}
			}
		}
		return modified;
	}

	private Set<TypeConstraint> findMembersToExtendTo(String supertype) {
		Set<TypeConstraint> membersToExtendTo = new HashSet<TypeConstraint>();
		Construction supertypeCxn = grammar.getConstruction(supertype);
		TypeConstraint mPole = supertypeCxn.getMeaningBlock().getTypeConstraint();
		if (mPole != null) {
			TypeSystem<?> ts = mPole.getTypeSystem();
			try {
				Set<String> relatedMPoles = ts.getAllSubtypes(mPole.getType());
				for (String typeName : relatedMPoles) {

					Set<Construction> currentCategoryMembers = cxnTypeSystem.getChildren(supertypeCxn);

					TypeConstraint type = getCurrentTypeConstraint(typeName, ts);
					grammarTables.getMBlockCooccurrenceTable().setQueryExpander(null);
					Map<Substitution<TypeConstraint>, Set<TypeConstraint>> coveringTypes = grammarTables
							.getMBlockCooccurrenceTable().findCoveringTypes(type);
					Set<TypeConstraint> semanticallyRelatedCxns = null;
					assert (coveringTypes.size() == 1); // it should be, because no query expansion is used
					semanticallyRelatedCxns = coveringTypes.values().iterator().next();

					if (semanticallyRelatedCxns != null) {
						for (Construction currentMember : currentCategoryMembers) {
							semanticallyRelatedCxns.remove(cxnTypeSystem.getCanonicalTypeConstraint(currentMember.getName()));
						}
						// however, this list may consist of a mixture of phrases and words (e.g. if it's a process meaning)
						// what to enforce? that the common supertype of the semantically related one and the others in the
						// cat not be Important_Type?

						if (!semanticallyRelatedCxns.isEmpty()) {
							Set<String> currentCategoryMemberNames = new LinkedHashSet<String>();
							for (Construction c : currentCategoryMembers) {
								currentCategoryMemberNames.add(c.getName());
							}

							for (TypeConstraint semanticallyRelated : semanticallyRelatedCxns) {
								if (extendTo(supertypeCxn, currentCategoryMemberNames, semanticallyRelated)) {
									// how to extend the category? mPole type is compatible but meaning constraints have to be
									// all re-extracted.
									membersToExtendTo.add(semanticallyRelated);
								}
							}
						}
					}
				}

			}
			catch (TypeSystemException tse) {
				logger.warning("TypeSystemException while trying to expand category to semantically related words of "
						+ supertype + ". Message given: " + tse.getLocalizedMessage());
			}
		}
		return membersToExtendTo;
	}

	protected boolean extendTo(Construction supertypeCxn, Set<String> currentCategoryMemberNames,
			TypeConstraint semanticallyRelated) {
		if (!cxnTypeSystem.get(semanticallyRelated.getType()).isConcrete())
			return false;

		Set<String> newCategoryMembers = new LinkedHashSet<String>(currentCategoryMemberNames);
		newCategoryMembers.add(semanticallyRelated.getType());
		newCategoryMembers.add(supertypeCxn.getType());
		try {
			Collection<String> bestCommonSupertypes = cxnTypeSystem.allBestCommonSupertypes(newCategoryMembers);
			for (String givenCategory : ChildesLocalizer.GIVEN_CATEGORIES) {
				if (bestCommonSupertypes.contains(givenCategory)) {
					return false;
				}
			}
			return true;
		}
		catch (TypeSystemException tse) {
			logger.warning("TypeSystemException while trying to expand category to semantically related words of "
					+ semanticallyRelated.getType() + ". Message given: " + tse.getLocalizedMessage());
		}
		return false;
	}

	protected TypeConstraint getCurrentTypeConstraint(String type, TypeSystem<?> oldTS) {
		if (oldTS.getName().equals(ECGConstants.SCHEMA)) {
			return grammar.getSchemaTypeSystem().getCanonicalTypeConstraint(type);
		}
		else if (oldTS.getName().equals(ECGConstants.ONTOLOGY)) {
			return grammar.getOntologyTypeSystem().getCanonicalTypeConstraint(type);
		}
		else {
			logger.warning("Unknown type system encountered for the type " + type);
			return null;
		}
	}
}
