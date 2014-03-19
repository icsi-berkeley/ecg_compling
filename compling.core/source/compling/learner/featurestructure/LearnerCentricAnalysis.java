// =============================================================================
// File        : LearnerFeatureStructureSet.java
// Author      : emok
// Change Log  : Created on Feb 11, 2007
//=============================================================================

package compling.learner.featurestructure;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.annotation.childes.ChildesTranscript.ChildesClause;
import compling.grammar.ecg.ECGConstants;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.FeatureStructureSet;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.FeatureStructureUtilities.FeatureStructureFormatter;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.AnalysisVerifier.Scorecard;
import compling.learner.contextfitting.ContextFitter.ContextualFit;
import compling.learner.featurestructure.ResolutionResults.LCAResolution;
import compling.parser.ecgparser.AnalysisInContext;
import compling.parser.ecgparser.CxnalSpan;
import compling.util.Pair;

//=============================================================================

public class LearnerCentricAnalysis {

	boolean modified = false;
	LCATables tables = null;
	ResolutionResults resolutions = null;
	boolean recovered = false;

	double analyzerScore = 0;
	ContextualFit contextualFit = null;
	Pair<Scorecard, Scorecard> verifierScore = null;
	Set<AnalysisInContext> analyses = null;
	Map<Integer, CxnalSpan> cxnalSpans = null;
	int maxSlotID = -1;

	ChildesClause utteranceAnalyzed = null;
	String currentDS = null;
	String currentSpeechAct = null;
	String currentSpeechActType = null;
	String currentSpeaker = null;
	String currentAddressee = null;
	Collection<String> jointAttention = null;

	protected LearnerCentricAnalysis(ChildesClause utteranceAnalyzed, int maxSlotID, boolean recovered) {
		this.utteranceAnalyzed = utteranceAnalyzed;
		analyses = new LinkedHashSet<AnalysisInContext>();
		this.maxSlotID = maxSlotID;
		this.recovered = recovered;
	}

	public LearnerCentricAnalysis(ChildesClause utteranceAnalyzed, AnalysisInContext analysis, int maxSlotID,
			boolean recovered) {
		this(utteranceAnalyzed, maxSlotID, recovered);
		analyses.add(analysis.clone());
		resolutions = new ResolutionResults(this, analysis);
		cxnalSpans = aggregateCxnalSpans(analyses);
	}

	public LearnerCentricAnalysis(ChildesClause utteranceAnalyzed, Collection<AnalysisInContext> partialAnalyses,
			int maxSlotID, boolean recovered) {
		this(utteranceAnalyzed, maxSlotID, recovered);
		for (AnalysisInContext analysis : partialAnalyses) {
			analyses.add(analysis.clone());
		}
		resolutions = new ResolutionResults(this, partialAnalyses);
		cxnalSpans = aggregateCxnalSpans(analyses);
	}

	public Collection<FeatureStructureSet> getFeatureStructureSets() {
		Set<FeatureStructureSet> featureStructureSets = new HashSet<FeatureStructureSet>();
		for (AnalysisInContext analysis : analyses) {
			featureStructureSets.add(analysis.getFeatureStructure());
		}
		return featureStructureSets;
	}

	public Slot getSlot(int slotID) {
		for (AnalysisInContext analysis : analyses) {
			Slot slot = analysis.getFeatureStructure().getSlot(slotID);
			if (slot != null) {
				return slot;
			}
		}
		return null;
	}

	public Slot getSlot(int rootID, String slotChain) {
		return getSlot(rootID, new SlotChain(slotChain));
	}

	public Slot getSlot(int rootID, SlotChain slotChain) {
		Slot root = null;
		AnalysisInContext analysisWithNamedRoot = null;

		for (AnalysisInContext analysis : analyses) {
			Slot slot = analysis.getFeatureStructure().getSlot(rootID);
			if (slot != null) {
				root = slot;
				analysisWithNamedRoot = analysis;
			}
		}

		if (root == null) {
			throw new LCAException("invalid root ID " + rootID + " supplied");
		}

		return analysisWithNamedRoot.getFeatureStructure().getSlot(root, slotChain);
	}

	public TypeConstraint getTypeConstraint(int slotID) {
		return getSlot(slotID) == null ? null : getSlot(slotID).getTypeConstraint();
	}

	public int getLargestAssignedSlotID() {
		return maxSlotID;
	}

	public boolean coindexAcrossFeatureStructureSets(FeatureStructureSet fss1, SlotChain sc1, FeatureStructureSet fss2,
			SlotChain sc2) {
		AnalysisInContext a1 = findAnalysis(fss1);
		AnalysisInContext a2 = findAnalysis(fss2);

		if (fss1.coindexAcrossFeatureStructureSets(sc1, sc2, fss2)) {
			a1.getSpans().addAll(a2.getSpans());
			analyses.remove(a2);
			return true;
		}
		return false;
	}

	public Collection<AnalysisInContext> getAnalyses() {
		return analyses;
	}

	public int getNumSeparateAnalyses() {
		return analyses.size();
	}

	protected AnalysisInContext findAnalysis(FeatureStructureSet fss) {
		for (AnalysisInContext analysis : getAnalyses()) {
			if (analysis.getFeatureStructure() == fss) {
				return analysis;
			}
		}
		return null;
	}

	public int getFeatureStructureSetIndex(FeatureStructureSet fss) {
		int i = 0;
		for (AnalysisInContext analysis : getAnalyses()) {
			if (analysis.getFeatureStructure() == fss) {
				return i;
			}
			i++;
		}
		return -1;
	}

	protected Map<Integer, CxnalSpan> aggregateCxnalSpans(Collection<AnalysisInContext> analyses) {
		Map<Integer, CxnalSpan> spans = new HashMap<Integer, CxnalSpan>();
		for (AnalysisInContext analysis : analyses) {
			List<CxnalSpan> list = analysis.getSpans();
			for (CxnalSpan span : list) {
				spans.put(span.getSlotID(), span);
				// Note that this includes omitted constituents, whose left and right spans are the same.
			}
		}
		return spans;
	}

	public Map<Integer, CxnalSpan> getCxnalSpans() {
		return cxnalSpans;
	}

	public Set<Construction> getCxnsUsed() {
		Set<Construction> cxns = new HashSet<Construction>();
		for (CxnalSpan span : cxnalSpans.values()) {
			if (span.getType() != null) // it's null if it's omitted
				cxns.add(span.getType());
		}
		return cxns;
	}

	public Set<Integer> findDummyFillerSlots() {

		Set<Integer> dummy = new HashSet<Integer>();
		for (AnalysisInContext analysis : analyses) {
			for (Slot slot : analysis.getFeatureStructure().getSlots()) {
				if (slot.getTypeConstraint() != null
						&& slot.getTypeConstraint().getTypeSystem().getName().equals(ECGConstants.CONSTRUCTIONAL)) {
					if (cxnalSpans.keySet().contains(slot.getID())) {
						// this is a real cxn slot
					}
					else {
						// this is a dummy
						removeConstituent(dummy, slot);
					}
				}
			}
		}

		return dummy;
	}

	protected void removeConstituent(Set<Integer> dummy, Slot slot) {
		if (!slot.hasStructuredFiller()) {
			return;
		}
		else {
			// System.out.println(" {has structured filler}");
			for (Role role : slot.getFeatures().keySet()) {
				// System.out.println("\t" + role.getName() + " <--- " +
				// slot.getFeatures().get(role).getParentSlots().size());
				removeConstituentHelper(dummy, slot, role);
			}
		}
	}

	protected void removeConstituentHelper(Set<Integer> dummy, Slot parentSlot, Role role) {
		Slot slot = parentSlot.getFeatures().get(role);
		int numPointers = slot.getParentSlots().size();
		if (numPointers == 1) {
			if (dummy.contains(parentSlot) || parentSlot.getTypeConstraint() != null
					&& parentSlot.getTypeConstraint().getTypeSystem().getName().equals(ECGConstants.CONSTRUCTIONAL)) {
				dummy.add(slot.getID());
				if (slot.hasStructuredFiller()) {
					for (Role itsrole : slot.getFeatures().keySet()) {
						removeConstituentHelper(dummy, slot, itsrole);
					}
				}
			}
		}
		else if (numPointers == 2) {
			if (slot.getTypeConstraint() != null && role.getTypeConstraint() != null
					&& slot.getTypeConstraint().getType() == role.getTypeConstraint().getType()) {
				// no further type constraints has been added to the slot --> Most probably dummy
				dummy.add(slot.getID());
				if (slot.hasStructuredFiller()) {
					for (Role itsrole : slot.getFeatures().keySet()) {
						removeConstituentHelper(dummy, slot, itsrole);
					}
				}
			}
		}

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (AnalysisInContext analysis : getAnalyses()) {
			sb.append(analysis.toString());
		}
		return sb.toString();
	}

	public LCAResolution getResolution(int slotID) {
		return resolutions.getResult(slotID);
	}

	public ResolutionResults getResolutionResults() {
		return resolutions;
	}

	public LCATables getTables() {
		if (tables == null) {
			tables = new LCATables(this);
		}
		return tables;
	}

	public ChildesClause getUtteranceAnalyzed() {
		return utteranceAnalyzed;
	}

	public double getAnalyzerScore() {
		return analyzerScore;
	}

	public void setAnalyzerScore(double analyzerScore) {
		this.analyzerScore = analyzerScore;
	}

	public ContextualFit getContextualFit() {
		return contextualFit;
	}

	public void setContextualFit(ContextualFit contextualFit) {
		this.contextualFit = contextualFit;
	}

	public Pair<Scorecard, Scorecard> getVerifierScore() {
		return verifierScore;
	}

	public void setVerifierScore(Scorecard verbArgScore, Scorecard argStructScore) {
		verifierScore = new Pair<Scorecard, Scorecard>(verbArgScore, argStructScore);
	}

	public boolean isRecovered() {
		return recovered;
	}

	public void setFormatter(FeatureStructureFormatter formatter) {
		FeatureStructureSet.setFormatter(formatter);
	}

	public String getCurrentDS() {
		return currentDS;
	}

	public void setCurrentDS(String currentDS) {
		this.currentDS = currentDS;
	}

	public String getCurrentSpeechAct() {
		return currentSpeechAct;
	}

	public void setCurrentSpeechAct(String currentSpeechAct) {
		this.currentSpeechAct = currentSpeechAct;
	}

	public String getCurrentSpeechActType() {
		return currentSpeechActType;
	}

	public void setCurrentSpeechActType(String currentSpeechActType) {
		this.currentSpeechActType = currentSpeechActType;
	}

	public String getCurrentSpeaker() {
		return currentSpeaker;
	}

	public void setCurrentSpeaker(String currentSpeaker) {
		this.currentSpeaker = currentSpeaker;
	}

	public String getCurrentAddressee() {
		return currentAddressee;
	}

	public void setCurrentAddressee(String currentAddressee) {
		this.currentAddressee = currentAddressee;
	}

	public Collection<String> getJointAttention() {
		return jointAttention;
	}

	public void setJointAttention(Collection<String> jointAttention) {
		this.jointAttention = jointAttention;
	}

}
