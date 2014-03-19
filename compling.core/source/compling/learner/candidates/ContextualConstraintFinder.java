// =============================================================================
//File        : CompositionFinder.java
//Author      : emok
//Change Log  : Created on Apr 5, 2007
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.ecg.Grammar.Schema;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.AnalysisVerifier.Correctness;
import compling.learner.LearnerException;
import compling.learner.LearnerGrammar;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.util.LearnerUtilities;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.MapSet;
import compling.util.Pair;

//=============================================================================

public class ContextualConstraintFinder extends CompositionFinder {

	static final Role mPole = new Role(ECGConstants.MEANING_POLE);
	static Logger logger = Logger.getLogger(ContextualConstraintFinder.class.getName());

	public ContextualConstraintFinder(LearnerGrammar learnerGrammar, LearnerCentricAnalysis lca, boolean useGoldStandard) {
		super(learnerGrammar, lca, useGoldStandard);
	}

	public List<CompositionCandidate> findCandidates(ContextualFit fit, List<CxnalSpan> usedSpans) {

		// index the sharedFillers (to double check that the event role filler doesn't have a corresponding form)
		MapSet<String, Integer> sharedFillers = new MapSet<String, Integer>();
		Set<Integer> fittedSlots = fit.getSlots();
		for (Integer slotID : fittedSlots) {
			if (fit.getCandidate(slotID) != null
					&& (!useGoldStandard || fit.getVerificationResults(slotID) != Correctness.INCORRECT)) {
				sharedFillers.put(fit.getCandidate(slotID), slotID);
			}
		}

		Set<Integer> contextuallyBoundRoles = new HashSet<Integer>();

		for (Integer cxnSlotID : lca.getCxnalSpans().keySet()) {
			CxnalSpan span = lca.getCxnalSpans().get(cxnSlotID);
			Slot slot = lca.getSlot(cxnSlotID).getSlot(mPole);
			TypeConstraint t = slot.getTypeConstraint();
			if (t != null && LearnerUtilities.isProcess(grammar, t)) {
				Schema process = grammar.getSchemaTypeSystem().get(t.getType());
				// heuristically selects core roles
				Set<String> coreRoleNames = grammarTables.getCoreRolesTable().get(process.getName());
				for (String core : coreRoleNames) {
					Slot roleSlot = slot.getFeatures().get(process.getRole(core));
					if (roleSlot != null && !contextuallyBoundRoles.contains(roleSlot.getID())) {
						String roleFiller = fit.getCandidate(roleSlot.getID());
						if (roleFiller != null && fit.getVerificationResults(roleSlot.getID()) != Correctness.INCORRECT
								&& sharedFillers.get(roleFiller).size() == 1) {
							contextuallyBoundRoles.add(roleSlot.getID());
						}
					}

				}
			}
		}

		MapSet<Integer, Pair<Integer, SlotChain>> slotChainTable = lcaTables.getSlotChainTable();
		MapSet<CxnalSpan, Pair<Integer, ECGSlotChain>> possibleConstructions = new MapSet<CxnalSpan, Pair<Integer, ECGSlotChain>>();
		for (Integer contextuallyBoundRole : contextuallyBoundRoles) {
			Set<Integer> slots = new HashSet<Integer>();
			slots.add(contextuallyBoundRole);
			TreeMap<CxnalSpan, ECGSlotChain> treeMap = LearnerUtilities.pullOutCxnalSpans(lca, grammar, slots,
					slotChainTable, false);
			if (treeMap.size() == 0) {
				throw new LearnerException("No constructions tied to the contextually bound role "
						+ lca.getSlot(contextuallyBoundRole));
			}
			else if (treeMap.size() > 1) {
				// more than one lexical item leading to the contextually bound role?
				logger.finer("more than one construction leading to the contextually bound role "
						+ lca.getSlot(contextuallyBoundRole));
			}
			CxnalSpan futureConstituent = treeMap.keySet().iterator().next();
			if (!usedSpans.contains(futureConstituent) && !futureConstituent.omitted()) {
				possibleConstructions.put(futureConstituent,
						new Pair<Integer, ECGSlotChain>(contextuallyBoundRole, treeMap.get(futureConstituent)));
			}
		}

		List<CompositionCandidate> tentativeCandidates = new ArrayList<CompositionCandidate>();
		for (CxnalSpan constituent : possibleConstructions.keySet()) {
			CompositionCandidate candidate = new CompositionCandidate(grammar);
			candidate.add(constituent);
			tentativeCandidates.add(candidate);
		}

		List<CompositionCandidate> candidates = new ArrayList<CompositionCandidate>();
		for (CompositionCandidate remainingCandidate : tentativeCandidates) {
			candidates.addAll(createMeaningfulCxn(remainingCandidate));
		}

		return candidates;
	}
}
