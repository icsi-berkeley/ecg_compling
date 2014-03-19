// =============================================================================
// File        : RevisionFinder.java
// Author      : emok
// Change Log  : Created on Apr 28, 2008
//=============================================================================

package compling.learner.candidates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.ecg.Grammar.ECGSlotChain;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.Constraint;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.learner.LearnerGrammar;
import compling.learner.featurestructure.LCATables;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.learner.grammartables.GrammarTables;
import compling.learner.learnertables.NGram;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Pair;
import compling.util.Triplet;

//=============================================================================

public class RevisionFinder {
	LearnerCentricAnalysis lca;
	LCATables lcaTables;

	LearnerGrammar learnerGrammar;
	Grammar grammar;
	GrammarTables grammarTables;
	NGram ngram;
	TypeSystem<Construction> cxnTypeSystem;

	ChildesClause utterance;

	static Role referentRole = null;
	static final double SEMANTIC_SCORE_THRESHOLD = -5.0;
	static final double OCCURENCE_THRESHOLD = 0.75;
	static CompositionCandidate.CxnalSpanComparator comp = new CompositionCandidate.CxnalSpanComparator();

	static Logger logger = Logger.getLogger(RevisionFinder.class.getName());

	public RevisionFinder(LearnerGrammar learnerGrammar, LearnerCentricAnalysis lca) {
		setGrammar(learnerGrammar);
		this.lca = lca;
		utterance = lca.getUtteranceAnalyzed(); // do I need this?
		lcaTables = lca.getTables();
	}

	public void setGrammar(LearnerGrammar learnerGrammar) {
		this.learnerGrammar = learnerGrammar;
		grammar = learnerGrammar.getGrammar();
		grammarTables = learnerGrammar.getGrammarTables();
		ngram = learnerGrammar.getNGram();
		cxnTypeSystem = grammar.getCxnTypeSystem();
	}

	public List<CompositionCandidate> findCandidates(CxnalSpan toRevise) {

		List<CompositionCandidate> revisions = new ArrayList<CompositionCandidate>();
		Construction cxnToRevise = toRevise.getType();

		// find the cxnal spans of interest -- spans that precede, follow, or fall inside the toRevise.
		Triplet<Set<CxnalSpan>, Set<CxnalSpan>, Map<String, CxnalSpan>> related = getRelatedSpans(toRevise);
		Set<CxnalSpan> touchingSpans = related.getFirst();
		Set<CxnalSpan> nonConstituentSpans = related.getSecond();
		Map<String, CxnalSpan> constituentSpans = related.getThird();

		Set<CxnalSpan> toAdd = new HashSet<CxnalSpan>();

		// FUTURE: it's a hacky heuristics. Maybe something like mutual information should go here.
		// check bigram
		for (CxnalSpan tSpan : touchingSpans) {
			// check the bigram with the construction to be revised
			double prob = getAppropriateNGram(tSpan, toRevise) / getAppropriateUnigram(tSpan, toRevise);
			if (prob > OCCURENCE_THRESHOLD) {
				toAdd.add(tSpan);
			}
		}

		for (CxnalSpan nSpan : nonConstituentSpans) {
			boolean aboveThreshold = false;
			// check the bigram with the constituents of the construction to be revised
			for (String cSpanName : constituentSpans.keySet()) {
				CxnalSpan cSpan = constituentSpans.get(cSpanName);
				double prob = getAppropriateNGram(nSpan, cSpan) / getAppropriateUnigram(nSpan, cSpan);
				if (prob > OCCURENCE_THRESHOLD) {
					aboveThreshold = true;
				}
			}
			if (aboveThreshold) {
				toAdd.add(nSpan);
			}
		}

		// make a new compositional candidate out of each addition
		for (CxnalSpan additionalConstituent : toAdd) {
			revisions.add(makeCompositionalCandidate(cxnToRevise, constituentSpans, additionalConstituent));
		}

		return revisions;
	}

	private int getAppropriateUnigram(CxnalSpan additionSpan, CxnalSpan existingSpan) {
//      if (comp.compare(additionSpan, existingSpan) <= 0) {
//         return ngram.getUnigram(additionSpan.getType().getName());
//      } else {
//         return ngram.getUnigram(existingSpan.getType().getName());
//      }
		return ngram.getUnigram(existingSpan.getType().getName());
	}

	private double getAppropriateNGram(CxnalSpan additionSpan, CxnalSpan existingSpan) {
		String n0, n1;
		if (comp.compare(additionSpan, existingSpan) <= 0) {
			n0 = additionSpan.getType().getName();
			n1 = existingSpan.getType().getName();
		}
		else {
			n0 = existingSpan.getType().getName();
			n1 = additionSpan.getType().getName();
		}
		int bigramCount = ngram.getBigram(n0, n1);
		return bigramCount;
	}

	protected CompositionCandidate makeCompositionalCandidate(Construction toRevise,
			Map<String, CxnalSpan> originalConstituents, CxnalSpan additionalConstituent) {

		// prepend, append or splice the new constituent among the constituents of the construction to be revised

		CompositionCandidate candidate = new CompositionCandidate(grammar);
		candidate.setParent(toRevise.getParents());
		candidate.setMBlockType(toRevise.getMeaningBlock().getTypeConstraint());
		candidate.addAll(originalConstituents.values());
		candidate.add(additionalConstituent);

		// dump the original meaning constraints into the new compositional candidate.
		for (Role r : toRevise.getMeaningBlock().getEvokedElements()) {
			candidate.addEvokes(r.clone());
		}

		for (Constraint meaningConstraint : toRevise.getMeaningBlock().getConstraints()) {
			if (meaningConstraint.getOperator().equals(ECGConstants.ASSIGN)) {
				SlotChain originalSC = meaningConstraint.getArguments().get(0);
				Pair<CxnalSpan, ECGSlotChain> copy = copySlotChain(originalConstituents, originalSC);
				Pair<Pair<CxnalSpan, ECGSlotChain>, String> assignmentConstraint = new Pair<Pair<CxnalSpan, ECGSlotChain>, String>(
						copy, meaningConstraint.getValue());
				candidate.addAssignmentConstraint(assignmentConstraint);
			}
			else if (meaningConstraint.getOperator().equals(ECGConstants.IDENTIFY)) {
				List<SlotChain> originalSCs = meaningConstraint.getArguments();
				List<Pair<CxnalSpan, ECGSlotChain>> newConstraint = new ArrayList<Pair<CxnalSpan, ECGSlotChain>>();
				for (SlotChain originalSC : originalSCs) {
					newConstraint.add(copySlotChain(originalConstituents, originalSC));
				}
				candidate.addUnificationConstraint(newConstraint);
			}
		}

		return candidate;
	}

	private Pair<CxnalSpan, ECGSlotChain> copySlotChain(Map<String, CxnalSpan> originalConstituents, SlotChain originalSC) {
		String constituent = null;
		ECGSlotChain slotChain = null;
		if (originalSC instanceof ECGSlotChain && ((ECGSlotChain) originalSC).startsWithSelf()) {
			constituent = null;
			slotChain = new ECGSlotChain(originalSC.toString());
		}
		else {
			constituent = originalSC.getChain().get(0).getName();
			slotChain = new ECGSlotChain().setChain(originalSC.getChain());
		}
		CxnalSpan span = constituent == null ? null : originalConstituents.get(constituent);
		return new Pair<CxnalSpan, ECGSlotChain>(span, slotChain);
	}

	protected Triplet<Set<CxnalSpan>, Set<CxnalSpan>, Map<String, CxnalSpan>> getRelatedSpans(CxnalSpan spanOfInterest) {
		int left = spanOfInterest.getLeft();
		int right = spanOfInterest.getRight();

		Set<CxnalSpan> touchingSpans = new HashSet<CxnalSpan>();
		Set<CxnalSpan> nonConstituentSpans = new HashSet<CxnalSpan>();
		Map<String, CxnalSpan> constituentSpans = new HashMap<String, CxnalSpan>();

		for (CxnalSpan span : lca.getCxnalSpans().values()) {

			if (span == spanOfInterest)
				continue;
			if (span.omitted() || span.getLeft() == -1 || span.getRight() < left || span.getLeft() > right)
				continue;

			Construction cxn = span.getType();
			if (!isUsefulCxnalType(cxn.getName()))
				continue;

			if (span.getRight() == left) { // but don't want things lke Leftover Morpheme or Root
				touchingSpans.add(span);
			}
			else if (span.getLeft() == right) {
				touchingSpans.add(span);
			}
			else {
				// this span falls inside the span of the spanOfInterest
				// need to figure out if this is a constituent of the spanOfInterest or not.
				if (lca.getTables().isInTheConstructionalTreeUnder(span, spanOfInterest)) {
					Map<Role, Slot> directConstituents = lca.getSlot(spanOfInterest.getSlotID()).getFeatures();
					Slot descendentSlot = lca.getSlot(span.getSlotID());
					for (Role r : directConstituents.keySet()) {
						if (directConstituents.get(r) == descendentSlot) {
							constituentSpans.put(r.getName(), span);
						}
					}
				}
				else {
					// this is not a constituent of the spanOfInterest.
					// this can be an addition to the construction under revision.
					nonConstituentSpans.add(span);
				}
			}
		}
		return new Triplet<Set<CxnalSpan>, Set<CxnalSpan>, Map<String, CxnalSpan>>(touchingSpans, nonConstituentSpans,
				constituentSpans);
	}

	public boolean isUsefulCxnalType(String type) {
		return !(type.equals(ChildesLocalizer.LEFTOVER_MORPHEME) || type.equals(ECGConstants.ROOT));
	}

}
