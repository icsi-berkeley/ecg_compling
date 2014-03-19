// =============================================================================
//File        : CompositionFinder.java
//Author      : emok
//Change Log  : Created on Apr 5, 2007
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.grammartables.ConstituentCooccurenceTable;
import compling.learner.grammartables.GrammarTables;
import compling.learner.grammartables.MBlockCooccurrenceTable;
import compling.learner.grammartables.SimpleExpander;
import compling.learner.grammartables.SimpleExpander.ExpansionType;
import compling.learner.grammartables.Substitution;
import compling.learner.learnertables.GeneralizationHistory;
import compling.learner.util.LearnerUtilities;
import compling.learner.util.LearnerUtilities.EqualsMappingFunction;
import compling.parser.ecgparser.Analysis;
import compling.util.Pair;

//=============================================================================

public class GeneralizationFinder implements Iterator<GeneralizationCandidate> {

	public static interface IndexingStrategy {
		/***
		 * returns a list of compatible constructions along with any shared roles that are constrained in the mapping
		 */
		public List<Pair<TypeConstraint, List<Role>>> retrieveConstructions(Construction baseType);
	}

	static int maxRelationsAllowed = 4;

	LearnerGrammar learnerGrammar = null;
	Grammar grammar = null;
	GrammarTables grammarTables = null;
	TypeSystem<Construction> cxnTypeSystem = null;

	List<String> seedConstructions;
	Set<String> generalizableWith = new LinkedHashSet<String>();

	IndexingStrategy strategy = null;
	GeneralizationCandidate nextCandidate = null;
	List<List<String>> previouslySuggested = new ArrayList<List<String>>();

	static Role mPole = new Role(ECGConstants.MEANING_POLE);

	private static Logger logger = Logger.getLogger(GeneralizationFinder.class.getName());

	public GeneralizationFinder(LearnerGrammar learnerGrammar) {
		this(learnerGrammar, null);
	}

	public GeneralizationFinder(LearnerGrammar learnerGrammar, Collection<String> seedConstructions) {
		setGrammar(learnerGrammar);
		this.seedConstructions = seedConstructions == null ? new ArrayList<String>() : new ArrayList<String>(
				seedConstructions);
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		grammarTables = learnerGrammar.getGrammarTables();
	}

	public void addSeed(String seed) {
		seedConstructions.add(0, seed);
	}

	public boolean hasNext() {
		nextCandidate = retrieveNext();
		return nextCandidate != null;
	}

	protected GeneralizationCandidate retrieveNext() {
		strategy = new IndexBySharedMeaning(grammar, grammarTables.getMBlockCooccurrenceTable());
		Iterator<String> i = seedConstructions.iterator();
		GeneralizationCandidate candidate = null;
		while (candidate == null && i.hasNext()) {
			Construction srcCxn = grammar.getConstruction(i.next());
			if (srcCxn == null) { // perhaps the seeds is purged from the grammar in the last iteration
				i.remove();
			}
			else {
				candidate = findPairwiseCandidate(strategy, srcCxn);
				if (candidate == null) {
					// if a seed produces no generalizations now, it won't at least until more cxns are learned, so remove
					// it.
					i.remove();
				}
			}
		}
		return candidate;
	}

	public GeneralizationCandidate next() {
		if (nextCandidate == null) { // there's a chance that the user has not called hasNext(), so try getting the next
												// one
			nextCandidate = retrieveNext();
		}
		if (nextCandidate != null) {
			List<String> types = new ArrayList<String>();
			types.add(nextCandidate.getSourceType().getType());
			types.add(nextCandidate.getTargetType().getType());
			previouslySuggested.add(types);
		}

		GeneralizationCandidate toReturn = nextCandidate;
		nextCandidate = null;
		return toReturn;
	}

	public void remove() {
		throw new UnsupportedOperationException("the remove operation is not supported by Generalization Finder.");
	}

	protected boolean hasBeenPreviouslySuggested(TypeConstraint a, TypeConstraint b) {
		for (List<String> previous : previouslySuggested) {
			// it's not as simple as this because these type constraints are from different type systems.
			if (previous.contains(a.getType()) && previous.contains(b.getType())) {
				return true;
			}
		}
		return false;
	}

	private GeneralizationCandidate findPairwiseCandidate(IndexingStrategy strategy, Construction srcCxn) {

		TypeConstraint srcType = srcCxn.getCxnTypeSystem().getCanonicalTypeConstraint(srcCxn.getName());
		GeneralizationCandidate candidate = null;
		List<Pair<TypeConstraint, List<Role>>> retrievedCxns = strategy.retrieveConstructions(srcCxn);

		GeneralizationHistory history = learnerGrammar.getGeneralizationHistory();

		List<String> retrievedCxnNames = new ArrayList<String>();

		for (ListIterator<Pair<TypeConstraint, List<Role>>> iter = retrievedCxns.listIterator(); iter.hasNext();) {
			Pair<TypeConstraint, List<Role>> i = iter.next();
			if (i.getFirst() == srcType) {
				iter.remove();
			}
			else {
				retrievedCxnNames.add(i.getFirst().getType());
			}
		}

		List<Pair<TypeConstraint, List<Role>>> notFurtherGeneralized = new ArrayList<Pair<TypeConstraint, List<Role>>>();
		for (ListIterator<String> iIter = retrievedCxnNames.listIterator(); iIter.hasNext();) {
			String i = iIter.next();
			boolean beenGeneralized = false;
			for (Iterator<String> jIter = retrievedCxnNames.iterator(); !beenGeneralized && jIter.hasNext();) {
				String j = jIter.next();
				beenGeneralized |= history.generalizes(j, i);
			}
			if (!beenGeneralized) {
				notFurtherGeneralized.add(retrievedCxns.get(iIter.previousIndex()));
			}
		}

		ListIterator<Pair<TypeConstraint, List<Role>>> iter = notFurtherGeneralized.listIterator();
		while (candidate == null && iter.hasNext()) {
			Pair<TypeConstraint, List<Role>> retrievedCxn = iter.next();

			TypeConstraint tgtType = retrievedCxn.getFirst();
			if (hasBeenPreviouslySuggested(srcType, tgtType))
				continue;

			Construction tgtCxn = grammar.getConstruction(tgtType.getType());
			if (tgtCxn.getConstructionalBlock().getElements().size() == srcCxn.getConstructionalBlock().getElements()
					.size()
					&& (LearnerUtilities.subsumes(srcCxn, tgtCxn, learnerGrammar) || LearnerUtilities.subsumes(tgtCxn,
							srcCxn, learnerGrammar)))
				continue;
			candidate = generatePairwiseCandidate(srcType, tgtType, retrievedCxn.getSecond());
		}

		return candidate;
	}

	public GeneralizationCandidate generatePairwiseCandidate(TypeConstraint srcType, TypeConstraint tgtType,
			List<Role> constrainedRoles) {
		return generatePairwiseCandidate(learnerGrammar, srcType, tgtType, constrainedRoles, true);
	}

	/***
	 * This method assumes that the source and target constructions are "ready to go", i.e. they have been checked for
	 * previous generalizations, etc
	 */
	public static GeneralizationCandidate generatePairwiseCandidate(LearnerGrammar learnerGrammar,
			TypeConstraint srcType, TypeConstraint tgtType, List<Role> constrainedRoles, boolean wantReplacementOnly) {

		String srcName = srcType.getType();
		String tgtName = tgtType.getType();
		Construction srcCxn = learnerGrammar.getGrammar().getConstruction(srcName);
		Construction tgtCxn = learnerGrammar.getGrammar().getConstruction(tgtName);

		boolean srcL = isLexical(learnerGrammar, srcType);
		boolean tgtL = isLexical(learnerGrammar, tgtType);

		if (srcL && tgtL) {
			return new CategorizationCandidate(learnerGrammar, srcType, tgtType);
		}
		else if (srcL || tgtL) {
			return wantReplacementOnly ? null : new CategorizationCandidate(learnerGrammar, srcType, tgtType);
		}
		else if (learnerGrammar.getGeneralizationHistory().haveGeneralizations(srcName, tgtName)) {
			return null;
		}

		// Do an RD check. If # of RDs do not match (RDs roughly correspond to # of core arguments for clausal stuff),
		// then don't generalize
		Set<Role> srcRDs = findRDs(srcCxn);
		Set<Role> tgtRDs = findRDs(tgtCxn);
		if (srcRDs.size() != tgtRDs.size())
			return null;

		Set<Role> srcConstituents = srcCxn.getConstructionalBlock().getElements();
		Set<Role> tgtConstituents = tgtCxn.getConstructionalBlock().getElements();

		int sizeDiff = srcConstituents.size() - tgtConstituents.size();
		if (sizeDiff != 0 && srcConstituents.size() != 0 && tgtConstituents.size() != 0) {

			// the smaller construction should have more or equally specific form constraints than the bigger one
			boolean nearlyIdentical = sizeDiff < 0 ? areNearlyIdentical(learnerGrammar, srcCxn, tgtCxn, srcConstituents,
					tgtConstituents) : areNearlyIdentical(learnerGrammar, tgtCxn, srcCxn, srcConstituents, tgtConstituents);

			if (nearlyIdentical) {
				if (sizeDiff < 0) {
					logger.finer("potential omission candidate found: " + srcName + ", " + tgtName);
					learnerGrammar.addToNearlyIdenticalList(srcName, tgtName);
				}
				else {
					logger.finer("potential omission candidate found: " + tgtName + ", " + srcName);
					learnerGrammar.addToNearlyIdenticalList(tgtName, srcName);
				}
			}
			else {
				if (sizeDiff < 0) {
					return generateDifferingLengthCandidate(learnerGrammar, srcType, tgtType, srcName, tgtName, srcCxn,
							tgtCxn, srcConstituents, tgtConstituents, constrainedRoles, wantReplacementOnly);
				}
				else {
					return generateDifferingLengthCandidate(learnerGrammar, tgtType, srcType, tgtName, srcName, tgtCxn,
							srcCxn, tgtConstituents, srcConstituents, constrainedRoles, wantReplacementOnly);
				}
			}

			return null;

		}
		else if (sizeDiff == 0) {
			return generateEqualLengthCandidate(learnerGrammar, srcType, tgtType, srcName, tgtName, srcCxn, tgtCxn,
					srcConstituents, tgtConstituents, constrainedRoles, wantReplacementOnly);
		}
		return null;
	}

	private static GeneralizationCandidate generateDifferingLengthCandidate(LearnerGrammar learnerGrammar,
			TypeConstraint shorterType, TypeConstraint longerType, String shorterName, String longerName,
			Construction shorterCxn, Construction longerCxn, Set<Role> shorterConstituents, Set<Role> longerConstituents,
			List<Role> constrainedRoles, boolean wantReplacementOnly) {

		if (constrainedRoles == null)
			constrainedRoles = new ArrayList<Role>();
		if (!LearnerUtilities.subsumes(shorterCxn, longerCxn, learnerGrammar)) {

			// This is a shallow semantic check
			List<Map<Role, Role>> constituentMap = LearnerUtilities.constrainedMapConstituents(shorterConstituents,
					longerConstituents, constrainedRoles);
			List<Boolean> srcTgtSyn = LearnerUtilities.syntacticallySubsumes(shorterCxn, longerCxn, constituentMap);
			List<List<Constraint>> srcTgtFailingConstraints = haveConsistentRoleFillerRelations(learnerGrammar,
					shorterCxn, longerCxn, constituentMap);
			List<Boolean> srcTgtSem = sortThroughFailingConstraints(srcTgtFailingConstraints);

			// the subsumed one has fewer constraints than the subsuming one
			for (int i = 0; i < srcTgtSyn.size(); i++) {
				if (srcTgtSyn.get(i) && srcTgtSem.get(i)) {
					GeneralizedReplacementCandidate candidate = new GeneralizedReplacementCandidate(learnerGrammar,
							shorterType, longerType, new ArrayList<Role>());
					candidate.setConstituentMapping(shorterType, constituentMap.get(i));
					return candidate;
				}
				else if (srcTgtSyn.get(i) && !srcTgtSem.get(i) || !srcTgtSyn.get(i) && srcTgtSem.get(i)) {
					// form lines up but meaning does not
					if (!wantReplacementOnly) {
						return new CategorizationCandidate(learnerGrammar, shorterType, longerType);
					}
				}
			}
		}
		return null;
	}

	private static GeneralizationCandidate generateEqualLengthCandidate(LearnerGrammar learnerGrammar,
			TypeConstraint srcType, TypeConstraint tgtType, String srcName, String tgtName, Construction srcCxn,
			Construction tgtCxn, Set<Role> srcConstituents, Set<Role> tgtConstituents, List<Role> constrainedRoles,
			boolean wantReplacementOnly) {

		// This is a shallow semantic check
		if (constrainedRoles == null)
			constrainedRoles = new ArrayList<Role>();
		List<Map<Role, Role>> constituentMap = LearnerUtilities.constrainedMapConstituents(srcConstituents,
				tgtConstituents, constrainedRoles);
		List<Map<Role, Role>> reverseMap = LearnerUtilities.reverseMappings(constituentMap);

		List<Boolean> srcTgtSyn = LearnerUtilities.syntacticallySubsumes(srcCxn, tgtCxn, constituentMap);
		List<Boolean> tgtSrcSyn = LearnerUtilities.syntacticallySubsumes(tgtCxn, srcCxn, reverseMap);
		List<List<Constraint>> srcTgtFailingConstraints = haveConsistentRoleFillerRelations(learnerGrammar, srcCxn,
				tgtCxn, constituentMap);
		List<List<Constraint>> tgtSrcFailingConstraints = haveConsistentRoleFillerRelations(learnerGrammar, tgtCxn,
				srcCxn, reverseMap);
		List<Boolean> srcTgtSem = sortThroughFailingConstraints(srcTgtFailingConstraints);
		List<Boolean> tgtSrcSem = sortThroughFailingConstraints(tgtSrcFailingConstraints);

		List<Map<Role, Role>> viableMappings = new ArrayList<Map<Role, Role>>();

		boolean checkChaining = false, onlyFormLinesUp = false, onlyMeaningLinesUp = false;

		// the subsumed one has fewer constraints than the subsuming one
		for (int i = 0; i < srcTgtSyn.size(); i++) {
			if (srcTgtSyn.get(i) && srcTgtSem.get(i)) {
				GeneralizedReplacementCandidate candidate = new GeneralizedReplacementCandidate(learnerGrammar, srcType,
						tgtType, new ArrayList<Role>());
				candidate.setConstituentMapping(srcType, constituentMap.get(i));
				return candidate;
			}
			else if (tgtSrcSyn.get(i) && tgtSrcSem.get(i)) {
				GeneralizedReplacementCandidate candidate = new GeneralizedReplacementCandidate(learnerGrammar, tgtType,
						srcType, new ArrayList<Role>());
				candidate.setConstituentMapping(tgtType, reverseMap.get(i));
				return candidate;
			}
			else if ((!srcTgtSyn.get(i) && !srcTgtSem.get(i)) && (!tgtSrcSyn.get(i) && !tgtSrcSem.get(i))) {
				// neither lines up, so no generalization. But this could be a good candidate for chaining (e.g. "ni3 mo2",
				// "mo2 you2"),
				// IF NO OTHER VIABLE MAPPINGS ARE FOUND
				checkChaining = true;
			}
			else if ((srcTgtSyn.get(i) && !srcTgtSem.get(i)) || (tgtSrcSyn.get(i) && !tgtSrcSem.get(i))) {
				// form lines up but meaning does not
				if (wantReplacementOnly) {
					onlyFormLinesUp = true;
				}
				else {
					return new CategorizationCandidate(learnerGrammar, srcType, tgtType);
				}
			}
			else if ((!srcTgtSyn.get(i) && srcTgtSem.get(i)) || (!tgtSrcSyn.get(i) && tgtSrcSem.get(i))) {
				if (wantReplacementOnly) {
					// meaning lines up but form does not
					onlyMeaningLinesUp = true;
				}
				else {
					return new CategorizationCandidate(learnerGrammar, srcType, tgtType);
				}
			}
		}

		if (viableMappings.isEmpty()) {
			if (checkChaining && isSuitableForChaining(learnerGrammar, srcType, tgtType)) {
				logger.finer("potential chaining candidate found: " + srcName + ", " + tgtName);
				learnerGrammar.addToChainable(srcName, tgtName);
			}
			else if (onlyFormLinesUp && onlyMeaningLinesUp) {
				// Different mappings produce different kinds of mismatch. This is probably spurious; do nothing for now.
			}
			else if ((onlyFormLinesUp || onlyMeaningLinesUp)
					&& requiresRevision(learnerGrammar, srcCxn, tgtCxn, srcConstituents, tgtConstituents)) {
				logger.finer("potential revision candidate found: " + srcName + ", " + tgtName);
				learnerGrammar.addToWatchList(srcName, tgtName);
			}
		}

		return null;
	}

	private static List<Boolean> sortThroughFailingConstraints(List<List<Constraint>> conflictingConstraintsByMap) {
		List<Boolean> srcTgtSem = new ArrayList<Boolean>();

		for (List<Constraint> conflictingConstraints : conflictingConstraintsByMap) {
			if (conflictingConstraints.size() < maxRelationsAllowed) {
				srcTgtSem.add(true);
			}
			else {
				for (Iterator<Constraint> i = conflictingConstraints.iterator(); i.hasNext();) {
					Constraint conflict = i.next();
					List<Role> chain = conflict.getArguments().get(0).getChain();
					if (conflict.getOperator().equals(ECGConstants.ASSIGN)
							&& chain.get(chain.size() - 1).getName().equals(ChildesLocalizer.eventTypeRoleName)) {
						i.remove();
					}
				}
				if (conflictingConstraints.size() < maxRelationsAllowed) {
					srcTgtSem.add(true);
				}
				else {
					srcTgtSem.add(false);
				}
			}
		}
		return srcTgtSem;
	}

	// Unfortunately, because of compound words in the Chinese grammar, the learner assumes that special
	// constructions exists in the grammar to denote lexical items, namely, morphemes and words

	protected static boolean isLexical(LearnerGrammar learnerGrammar, TypeConstraint aTS) {
		TypeSystem<Construction> cxnTS = learnerGrammar.getGrammar().getCxnTypeSystem();
		String morpheme = cxnTS.getInternedString(ChildesLocalizer.MORPHEME);
		String word = cxnTS.getInternedString(ChildesLocalizer.WORD);
		String a = cxnTS.getInternedString(aTS.getType());
		try {
			return cxnTS.subtype(a, morpheme) || cxnTS.subtype(a, word);
		}
		catch (TypeSystemException tse) {
			throw new LearnerException("Something went wrong while checking for generalizaton candidates", tse);
		}
	}

	protected static Set<Role> findRDs(Construction cxn) {
		Set<Role> rds = new HashSet<Role>();
		for (Role r : cxn.getMeaningBlock().getEvokedElements()) {
			if (r.getTypeConstraint().getType().equals(ECGConstants.RD)) {
				rds.add(r);
			}
		}
		return rds;
	}

	private static boolean areNearlyIdentical(LearnerGrammar learnerGrammar, Construction srcCxn, Construction tgtCxn,
			Set<Role> srcConstituents, Set<Role> tgtConstituents) {

		List<Map<Role, Role>> constituentMap = LearnerUtilities.mapConstituents(srcConstituents, tgtConstituents,
				new EqualsMappingFunction());
		constituentMap = LearnerUtilities.retainSyntacticallySubsuming(srcCxn, tgtCxn, constituentMap);

		for (Iterator<Map<Role, Role>> iter = constituentMap.iterator(); iter.hasNext();) {
			Map<Role, Role> map = iter.next();
			Pair<Boolean, List<Constraint>> subsuming = LearnerUtilities.semanticallySubsumesGivenMapping(srcCxn, tgtCxn,
					map, learnerGrammar);
			if (!subsuming.getFirst() && subsuming.getSecond().size() < maxRelationsAllowed) {
				boolean canAllBeRelaxed = true;
				for (Constraint conflict : subsuming.getSecond()) {
					if (conflict.getOperator().equals(ECGConstants.ASSIGN)) {
						// can be remedied by with one single contextual constraint relaxation.
						Collection<String> relaxedTypes = GeneralizationCandidate.findRelaxedContextualConstraint(
								learnerGrammar.getGrammar(), conflict, learnerGrammar.getGrammarTables()
										.getConstructionCloneTable().getInstance(tgtCxn));
						if (relaxedTypes.isEmpty()) {
							canAllBeRelaxed = false;
						}
					}
					else {
						canAllBeRelaxed = false;
					}
				}
				if (!canAllBeRelaxed) {
					iter.remove();
				}
			}
			else if (!subsuming.getFirst()) {
				iter.remove();
			}
		}
		if (constituentMap.isEmpty())
			return false;
		return true;
	}

	protected static boolean isSuitableForChaining(LearnerGrammar learnerGrammar, TypeConstraint cxn1,
			TypeConstraint cxn2) {
		Map<TypeConstraint, Integer> cst1 = learnerGrammar.getGrammarTables().getConstituentLookupTable().get(cxn1);
		Map<TypeConstraint, Integer> cst2 = learnerGrammar.getGrammarTables().getConstituentLookupTable().get(cxn2);
		Set<TypeConstraint> cxn1Types = new HashSet<TypeConstraint>(cst1.keySet());
		cxn1Types.retainAll(cst2.keySet());
		return !cxn1Types.isEmpty();
	}

	protected static boolean requiresRevision(LearnerGrammar learnerGrammar, Construction srcCxn, Construction tgtCxn,
			Set<Role> srcConstituents, Set<Role> tgtConstituents) {

		// Can't use the equals mapping because it will rarely happen, if ever, that exactly the same constituents appear
		// in different orcer
		// instead make sure that the two constructions have the same meaning pole (are talking about the same event)
		TypeConstraint srcMPole = srcCxn.getMeaningBlock().getTypeConstraint();
		TypeConstraint tgtMPole = tgtCxn.getMeaningBlock().getTypeConstraint();
		if (srcMPole == null && tgtMPole == null) {
			// both null but at least the same.
			return true;
		}
		else if (srcMPole == null || tgtMPole == null) {
			// only one is null. Bad. (not that this should happen if the code gets to this point)
			return false;
		}
		else if (srcMPole == tgtMPole) {
			if (!srcMPole.getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				// if it's not an event descriptor, this is good enough
				return true;
			}
			else {
				TypeConstraint srcEventType = LearnerUtilities.findEventTypeRestriction(srcCxn,
						learnerGrammar.getGrammarTables());
				TypeConstraint tgtEventType = LearnerUtilities.findEventTypeRestriction(srcCxn,
						learnerGrammar.getGrammarTables());
				return (srcEventType == tgtEventType);
			}
		}

		return false;
	}

	protected static List<List<Constraint>> haveConsistentRoleFillerRelations(LearnerGrammar learnerGrammar,
			Construction a, Construction b, List<Map<Role, Role>> aToBMaps) {
		List<List<Constraint>> results = new ArrayList<List<Constraint>>();
		for (Map<Role, Role> map : aToBMaps) {
			results.add(haveConsistentRoleFillerRelations(learnerGrammar, a, b, map));
		}
		return results;
	}

	protected static List<Constraint> haveConsistentRoleFillerRelations(LearnerGrammar learnerGrammar, Construction a,
			Construction b, Map<Role, Role> aToBMaps) {

		Analysis bAnalysis = learnerGrammar.getGrammarTables().getConstructionCloneTable().getInstance(b).clone();
		List<Constraint> failingConstraints = new ArrayList<Constraint>();
		Set<Role> aConstituents = a.getConstructionalBlock().getElements();

		for (Constraint aConstraint : a.getMeaningBlock().getConstraints()) {

			boolean toCheck = true;
			for (SlotChain sc : aConstraint.getArguments()) {
				// if it's not a self.m or a constituent.m slotchain, don't check because it's an evoked / constituent-local
				// role
				// it'll require all the recursive mapping machinary that makes up this class.
				if ((!sc.getChain().get(0).equals(mPole) && !aConstituents.contains(sc.getChain().get(0)))
						|| (aConstituents.contains(sc.getChain().get(0)) && !sc.getChain().get(1).equals(mPole)))
					toCheck = false;
			}

			if (toCheck) {
				Constraint mappedConstraint = LearnerUtilities.mapConstraint(aConstraint, aToBMaps);
				if (!LearnerUtilities.isInAnalysis(mappedConstraint, bAnalysis, learnerGrammar.getGrammar(), false)) {
					failingConstraints.add(aConstraint);
				}
			}
		}

		return failingConstraints;
	}

	public class IndexBySharedConstituents implements IndexingStrategy {

		ConstituentCooccurenceTable constituencyTable = null;

		public IndexBySharedConstituents(ConstituentCooccurenceTable constituencyTable) {
			this.constituencyTable = constituencyTable;
		}

		public List<Pair<TypeConstraint, List<Role>>> retrieveConstructions(Construction srcCxn) {

			List<Pair<TypeConstraint, List<Role>>> cxns = new ArrayList<Pair<TypeConstraint, List<Role>>>();
			Set<Role> srcConstituents = srcCxn.getConstructionalBlock().getElements();
			if (srcConstituents.size() == 0) {
				return cxns;
			}

			for (Role constrainedConstituent : srcConstituents) {
				List<Role> constrained = new ArrayList<Role>();
				constrained.add(constrainedConstituent);

				Map<Substitution<TypeConstraint>, Set<TypeConstraint>> subsumed = constituencyTable
						.findCoveringTypes(constrainedConstituent.getTypeConstraint());
				for (Substitution<TypeConstraint> substitution : subsumed.keySet()) {
					for (TypeConstraint cxnType : subsumed.get(substitution)) {
						cxns.add(new Pair<TypeConstraint, List<Role>>(cxnType, constrained));
					}
				}
			}
			return cxns;
		}

	}

	public class IndexBySharedMeaning implements IndexingStrategy {

		Grammar grammar;
		MBlockCooccurrenceTable mblockCooccurrenceTable;

		public IndexBySharedMeaning(Grammar grammar, MBlockCooccurrenceTable mblockCooccurrenceTable) {
			this.grammar = grammar;
			this.mblockCooccurrenceTable = mblockCooccurrenceTable;
		}

		public List<Pair<TypeConstraint, List<Role>>> retrieveConstructions(Construction srcCxn) {

			List<Pair<TypeConstraint, List<Role>>> cxns = new ArrayList<Pair<TypeConstraint, List<Role>>>();
			Set<TypeConstraint> retrievedCxns = new LinkedHashSet<TypeConstraint>();

			TypeConstraint mblockType = srcCxn.getMeaningBlock().getTypeConstraint();

			if (mblockType == null) {
				return cxns;
			}

			// if an Event Descriptor is the meaning pole of the construction, look for the type restriction on the event
			// type role
			if (mblockType.getType().equals(ChildesLocalizer.eventDescriptorTypeName)) {
				mblockType = LearnerUtilities.findEventTypeRestriction(srcCxn, grammarTables);
			}

			List<Role> constrained = new ArrayList<Role>();

			mblockCooccurrenceTable.setQueryExpander(new SimpleExpander(grammar, ExpansionType.Supertype));
			Map<Substitution<TypeConstraint>, Set<TypeConstraint>> subsumed = mblockCooccurrenceTable
					.findCoveringTypes(mblockType);

			for (Substitution<TypeConstraint> substitution : subsumed.keySet()) {
				for (TypeConstraint cxnType : subsumed.get(substitution)) {
					if (!retrievedCxns.contains(cxnType))
						cxns.add(new Pair<TypeConstraint, List<Role>>(cxnType, constrained));
					retrievedCxns.add(cxnType);
				}
			}

			mblockCooccurrenceTable.setQueryExpander(new SimpleExpander(grammar, ExpansionType.Sibling));
			Map<Substitution<TypeConstraint>, Set<TypeConstraint>> siblings = mblockCooccurrenceTable
					.findCoveringTypes(mblockType);

			for (Substitution<TypeConstraint> substitution : siblings.keySet()) {
				for (TypeConstraint cxnType : siblings.get(substitution)) {
					if (!retrievedCxns.contains(cxnType))
						cxns.add(new Pair<TypeConstraint, List<Role>>(cxnType, constrained));
					retrievedCxns.add(cxnType);
				}
			}

			return cxns;
		}

	}
}
