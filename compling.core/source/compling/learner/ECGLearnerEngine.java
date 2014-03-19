// =============================================================================
//File        : LearnerEngine.java
//Author      : emok
//Change Log  : Created on May 30, 2006
//=============================================================================

package compling.learner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.AnalysisVerifier.Cat;
import compling.learner.AnalysisVerifier.Correctness;
import compling.learner.LearnerException.InvalidGeneralizationException;
import compling.learner.LearnerGrammar.GrammarChanges;
import compling.learner.candidates.CategoryExpander;
import compling.learner.candidates.CategoryMerger;
import compling.learner.candidates.CompositionCandidate;
import compling.learner.candidates.CompositionFinder;
import compling.learner.candidates.ContextualConstraintFinder;
import compling.learner.candidates.GeneralizationCandidate;
import compling.learner.candidates.GeneralizationFinder;
import compling.learner.candidates.OmissionFinder;
import compling.learner.candidates.RevisionFinder;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.learnertables.ConstructionalSubtypeTable;
import compling.learner.learnertables.NGram;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Counter;
import compling.util.MapFactory.LinkedHashMapFactory;
import compling.util.MapMap;
import compling.util.MapSet;
import compling.util.SetFactory.LinkedHashSetFactory;

//=============================================================================

public class ECGLearnerEngine {

	public static enum Operation {
		revisionAttempts,
		revisionAccepted,
		omissionAttempts,
		omissionAccepted,
		compositionAttempts,
		compositionAccepted,

		generalizeAttempts,
		generalizeAccepted,
		categoryMergeAttempts,
		categoryMergeAccepted,
		categoryExpansionAttempts,
		categoryExpansionAccepted;
	}

	Counter<Operation> operationCounts = new Counter<Operation>();

	LearnerGrammar experimentalGrammar = null;
	boolean grammarModified = false;
	LearnerCentricAnalysis currentLCA = null;
	boolean useMDL;

	List<String> recentProposals = new ArrayList<String>();

	private static int counter = 0;

	private static final int LENGTH_WORTH_CACHING = 3;
	public static final String GEN_CXN_PREFIX = "genCxn";
	public static final String CAT_CXN_PREFIX = "Cat";
	public static final String LEX_CXN_SUFFIX = "-Cxn";

	private static Logger logger = Logger.getLogger(ECGLearnerEngine.class.getName());

	public ECGLearnerEngine(LearnerGrammar experimentalGrammar, boolean useMDL) {
		// public ECGLearnerEngine(ECGLearner learner, Grammar initialGrammar, Grammar initialGrammarCopy, GrammarTables
		// tables, BasicScorer semanticModel, boolean batchUpdate) {
		this.experimentalGrammar = experimentalGrammar;
		this.useMDL = useMDL;
		counter = findInitialCounter();
	}

	protected int findInitialCounter() {
		int maxCount = 0;
		for (Construction c : experimentalGrammar.getGrammar().getAllConstructions()) {
			int count = 0;
			if (c.getName().startsWith(GEN_CXN_PREFIX) || c.getName().startsWith(CAT_CXN_PREFIX)) {
				count = Integer.valueOf(c.getName().replace(GEN_CXN_PREFIX, "").replace(CAT_CXN_PREFIX, ""));
			}
			else if (c.getName().contains(LEX_CXN_SUFFIX)) {
				count = Integer.valueOf(c.getName().replaceAll(".*" + LEX_CXN_SUFFIX, ""));
			}
			if (count > maxCount) {
				maxCount = count;
			}
		}
		return maxCount;
	}

	public static int getCounter() {
		return ++counter;
	}

	public void learn(LearnerCentricAnalysis bestAnalysis, boolean useGoldStandard) throws IOException {

		recentProposals.clear();
		experimentalGrammar.updateTablesAfterAnalysis(bestAnalysis, true);
		ChildesClause clause = bestAnalysis.getUtteranceAnalyzed();
		if (clause.size() > LENGTH_WORTH_CACHING) {
			experimentalGrammar.cacheUtterance(clause);
		}

		if (!useGoldStandard || bestAnalysis.getVerifierScore() == null
				|| bestAnalysis.getVerifierScore().getFirst().getAbsoluteCount(Cat.CORE, Correctness.INCORRECT) == 0) {
			propose(bestAnalysis, useGoldStandard);
		}

		List<String> recentlyTouched = new ArrayList<String>(recentProposals);
		for (Construction c : bestAnalysis.getCxnsUsed()) {
			recentlyTouched.add(c.getName());
		}

		reorganizeConstructions(recentlyTouched);
	}

	public void propose(LearnerCentricAnalysis lca, boolean useGoldStandard) {
		this.currentLCA = lca;
		if (experimentalGrammar.hasWatchlist()) {
			reviseConstructions(currentLCA, currentLCA.getContextualFit());
		}
		if (experimentalGrammar.hasNearlyIdenticalConstructions()) {
			checkForOmission(currentLCA, currentLCA.getContextualFit());
		}
		composeConstructions(currentLCA, currentLCA.getContextualFit(), useGoldStandard);
	}

	public void reviseConstructions(LearnerCentricAnalysis lca, ContextualFit fit) {
		logger.finer("Checking analysis against watchlist");

		RevisionFinder finder = new RevisionFinder(experimentalGrammar, lca);

		MapSet<String, CompositionCandidate> revisions = new MapSet<String, CompositionCandidate>(
				new LinkedHashMapFactory<String, Set<CompositionCandidate>>(),
				new LinkedHashSetFactory<CompositionCandidate>());
		for (CxnalSpan span : lca.getCxnalSpans().values()) {
			if (span.getType() != null) { // it's null if it's omitted
				String toRevise = span.getType().getName();
				if (experimentalGrammar.isOnWatchList(toRevise)) {
					logger.finer("cxn used is on watch list: " + toRevise);
					revisions.putAll(toRevise, finder.findCandidates(span));
				}
			}
		}

		if (revisions.isEmpty())
			return;

		logger.info("Attempting revision");

		for (String toRevise : revisions.keySet()) {
			boolean canBeRemovedFromWatchList = false;
			for (CompositionCandidate candidate : revisions.get(toRevise)) {
				operationCounts.incrementCount(Operation.revisionAttempts, 1);
				candidate.setGrammar(experimentalGrammar.getGrammar());
				Construction newCxn = candidate.createNewConstruction();
				Set<Construction> subsuming = getSubsuming(newCxn, candidate.getConstituentTypes(), experimentalGrammar);
				if (subsuming.isEmpty()) {
					GrammarChanges changes = new GrammarChanges(newCxn);
					boolean success = experimentalGrammar.modifyGrammar(changes);
					if (success) {
						operationCounts.incrementCount(Operation.revisionAccepted, 1);
						grammarModified = true;
						logChanges(changes);
						canBeRemovedFromWatchList = true;
						recentProposals.add(newCxn.getName());
					}
				}
			}
			if (canBeRemovedFromWatchList) {
				experimentalGrammar.removeFromWatchList(toRevise);
			}

		}
	}

	public void checkForOmission(LearnerCentricAnalysis lca, ContextualFit fit) {

		Set<String> possibleOmission = new HashSet<String>();
		for (CxnalSpan span : lca.getCxnalSpans().values()) {
			if (span.getType() != null) {
				String toRevise = span.getType().getName();
				// if this is on the short end of the list, see if the crucial difference in constituent really can't be
				// found in the input.
				if (experimentalGrammar.isOnNearlyIdenticalList(toRevise)) {
					possibleOmission.add(toRevise);
				}
			}
		}

		if (!possibleOmission.isEmpty()) {
			logger.info("Attempting omissify");
			OmissionFinder finder = new OmissionFinder(experimentalGrammar, lca);
			for (String toRevise : possibleOmission) {
				operationCounts.incrementCount(Operation.omissionAttempts, 1);
				if (finder.findOmissible(toRevise, experimentalGrammar.getNearlyIdenticalList().get(toRevise))) {
					// omission doesn't necessarily involve changing the grammar. All it does is updating the locality table.
					// On the other hand, the shorter construction can be deleted.
					GrammarChanges changes = finder.getChanges();
					if (changes.isEmpty()) {
						operationCounts.incrementCount(Operation.omissionAccepted, 1);
						grammarModified = true;
						experimentalGrammar.removeFromNearlyIdenticalList(toRevise);
					}
					else {
						boolean success = experimentalGrammar.modifyGrammar(changes);
						if (success) {
							operationCounts.incrementCount(Operation.omissionAccepted, 1);
							grammarModified = true;
							logChanges(changes);
							experimentalGrammar.removeFromNearlyIdenticalList(toRevise);

							for (Construction c : changes.cxnsToReplace.values()) {
								recentProposals.add(c.getName());
							}
						}
					}
				}
			}
		}
	}

	public void composeConstructions(LearnerCentricAnalysis lca, ContextualFit fit, boolean useGoldStandard) {
		logger.info("Attempting compose");
		CompositionFinder finder = new CompositionFinder(experimentalGrammar, lca, useGoldStandard);
		List<CompositionCandidate> candidates = finder.findCandidates(fit, false); // do not include DS slotchains in the
																											// normal compose, except for RD
																											// bindings
		List<CxnalSpan> usedSpans = addCompositionCandidateIntoGrammar(candidates);

		ContextualConstraintFinder ccFinder = new ContextualConstraintFinder(experimentalGrammar, lca, useGoldStandard);
		List<CompositionCandidate> ccCandidates = ccFinder.findCandidates(fit, usedSpans);
		addCompositionCandidateIntoGrammar(ccCandidates);
	}

	private List<CxnalSpan> addCompositionCandidateIntoGrammar(List<CompositionCandidate> candidates) {
		List<CxnalSpan> usedSpans = new ArrayList<CxnalSpan>();
		for (CompositionCandidate candidate : candidates) {
			operationCounts.incrementCount(Operation.compositionAttempts, 1);
			candidate.setGrammar(experimentalGrammar.getGrammar());
			Construction newCxn = candidate.createNewConstruction();
			Set<Construction> subsuming = getSubsuming(newCxn, candidate.getConstituentTypes(), experimentalGrammar);
			if (subsuming.isEmpty()) {
				GrammarChanges changes = new GrammarChanges(newCxn);
				boolean success = experimentalGrammar.modifyGrammar(changes);
				if (success) {
					operationCounts.incrementCount(Operation.compositionAccepted, 1);
					grammarModified = true;
					logChanges(changes);
					usedSpans.addAll(candidate);
					recentProposals.add(newCxn.getName());
				}
			}
			else {
				// The current proposal is already in the experimental grammar. It's likely to happen if batch learning is
				// on,
				// because dialogue content tends to repeat but constructions proposed earlier aren't being put to use yet.
				// A reasonable thing to do is to up the counts of the subsuming

				// TODO: find a good way to up the probability of a new construction. Other than unigram there's no place to
				// really add it.
			}
		}
		return usedSpans;
	}

	public void reorganizeConstructions(Collection<String> specificConstructions) throws IOException {

		List<String> seeds = new ArrayList<String>();
		TypeSystem<Construction> cxnTypeSystem = experimentalGrammar.getGrammar().getCxnTypeSystem();
		String PHRASETYPE = cxnTypeSystem.getInternedString(ChildesLocalizer.PHRASE);
		String CLAUSETYPE = cxnTypeSystem.getInternedString(ChildesLocalizer.CLAUSE);

		for (String x : specificConstructions) {
			String cxnName = cxnTypeSystem.getInternedString(x);
			try {
				if (cxnTypeSystem.subtype(cxnName, PHRASETYPE) || (cxnTypeSystem.subtype(cxnName, CLAUSETYPE))) {
					seeds.add(cxnName);
				}
			}
			catch (TypeSystemException tse) {
				logger.warning("TypeSystemException caught when trying to seed generalization " + cxnName);
			}
		}

		if (seeds.isEmpty())
			return;

		logger.info("Attempting reorganization");
		generalizeConstructions(seeds);

		// expandCategories(seeds);
	}

	private void generalizeConstructions(List<String> seeds) throws IOException {
		GeneralizationFinder finder = new GeneralizationFinder(experimentalGrammar, seeds);

		while (finder.hasNext()) {
			operationCounts.incrementCount(Operation.generalizeAttempts, 1);
			GeneralizationCandidate candidate = finder.next();
			candidate.setGrammar(experimentalGrammar);
			candidate.createNewConstructions();
			if (!candidate.isViable())
				continue;

			GrammarChanges aggregrate = new GrammarChanges();
			GrammarChanges changes = candidate.getChanges();
			aggregrate.aggregateChanges(changes);

			logger.finer("viable generalization candidate found: " + candidate);

			try {
				boolean successfullyAdded = false;
				LearnerGrammar testGrammar = experimentalGrammar.makeCopy();
				successfullyAdded = testGrammar.modifyGrammar(changes);
				if (successfullyAdded) {
					operationCounts.incrementCount(Operation.generalizeAccepted, 1);
					Map<GeneralizationCandidate, MapMap<Role, TypeConstraint, Role>> finalRoleMapping = candidate
							.getFinalRoleMapping();
					for (GeneralizationCandidate c : finalRoleMapping.keySet()) {
						testGrammar.updateTablesAfterGeneralization(c.getNewCxnName(), c.getGeneralizedOver(),
								finalRoleMapping.get(c));
					}

					// go ahead and do all the merges triggered by the generalization (e.g. generalizing over one category
					// and one lex item)
					MapSet<String, String> triggeredMerges = candidate.getCategoriesToMerge();
					for (String supertype : triggeredMerges.keySet()) {
						operationCounts.incrementCount(Operation.categoryMergeAttempts, 1);
						if (shouldMerge(supertype, triggeredMerges.get(supertype))) {
							logger.info("Attempting category merge");
							CategoryMerger merger = new CategoryMerger(testGrammar, supertype, triggeredMerges.get(supertype));
							GrammarChanges subsequentMergeChanges = merger.mergeCategories();

							// caching the old tables since we are getting rid of some constructions and we need the old
							// numbers
							ConstructionalSubtypeTable oldCxnTable = testGrammar.getConstructionalSubtypeTable();
							NGram oldNGram = testGrammar.getNGram();
							boolean successfullyMerged = testGrammar.modifyGrammar(subsequentMergeChanges);

							if (successfullyMerged) {
								operationCounts.incrementCount(Operation.categoryMergeAccepted, 1);
								aggregrate.aggregateChanges(subsequentMergeChanges);
								testGrammar.updateTablesAfterCategoryMerge(supertype, triggeredMerges.get(supertype),
										oldCxnTable, oldNGram, false);
							}
						}
					}

					double currentDL = 0.0, newDL = 0.0;
					if (useMDL) {
						currentDL = experimentalGrammar.getDescriptionLength();
						newDL = testGrammar.getDescriptionLength();
						logger.fine("candidate = " + candidate + ";\n old DL = " + currentDL + "; new DL = " + newDL + "\n");
					}
					if (!useMDL || newDL <= currentDL) {
						experimentalGrammar = testGrammar;
						logChanges(aggregrate);
						finder.setGrammar(experimentalGrammar);
						grammarModified = true;
					}
				}

			}
			catch (InvalidGeneralizationException ige) {
				// do nothing for now
				logger.warning("Invalid Generalization: " + ige.getLocalizedMessage());
			}
		}
	}

	protected boolean shouldMerge(String supertypeName, Set<String> categories) {
		// if the supertype only consists of pronounciation variants, don't merge
		Iterator<String> i = categories.iterator();
		while (i.hasNext()) {
			if (i.next().contains(ChildesLocalizer.VARIANT_SUFFIX)) {
				i.remove();
			}
		}
		if (categories.isEmpty()) {
			return false;
		}
		return true;
	}

	private void expandCategories(List<String> seeds) {
		CategoryExpander expander = new CategoryExpander(experimentalGrammar, seeds);

	}

	public static Set<Construction> getSubsuming(Construction newCxn, List<Construction> constituentTypes,
			LearnerGrammar grammar) {
		GrammarTables tables = grammar.getGrammarTables();
		Set<Construction> cxnsToCheckAgainst = tables.getConstituentCooccurenceTable().findTypeWithConstituents(
				constituentTypes);
		return getSubsuming(grammar, newCxn, cxnsToCheckAgainst, tables);
	}

	public static Set<Construction> getSubsuming(LearnerGrammar grammar, Construction target,
			Collection<Construction> cxnsToCheckAgainst, GrammarTables tables) {
		Set<Construction> subsuming = new HashSet<Construction>();
		for (Construction cxn : cxnsToCheckAgainst) {
			boolean subsumes = LearnerUtilities.subsumes(cxn, target, grammar);
			if (subsumes) {
				subsuming.add(cxn);
			}
		}
		return subsuming;
	}

	protected void logChanges(GrammarChanges changes) {
		logger.info(changes.toString());
	}

	public LearnerGrammar getExperimentalGrammar() {
		return experimentalGrammar;
	}

	public LearnerCentricAnalysis getLCA() {
		return currentLCA;
	}

	public boolean isGrammarModified() {
		return grammarModified;
	}

	public void resetGrammarModified() {
		this.grammarModified = false;
	}

	public String getFinalOutput() {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		for (Operation op : Operation.values()) {
			sb.append(op.toString()).append(": ").append(operationCounts.getCount(op)).append("\t");
			i++;
			if (i % 2 == 0) {
				sb.append("\n");
			}
		}
		return sb.append("\n\n").toString();
	}
}
