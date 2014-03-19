// =============================================================================
// File        : ResolutionResults.java
// Author      : emok
// Change Log  : Created on May 23, 2007
//=============================================================================

package compling.learner.featurestructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compling.context.MiniOntology.Individual;
import compling.context.Resolution;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.parser.ecgparser.AnalysisInContext;

//=============================================================================

public class ResolutionResults {

	public static class LCAResolution {
		Map<String, Double> scores;
		Map<String, Individual> candidates;
		boolean resolved = false;
		boolean omitted = false;

		public LCAResolution() {
			scores = new HashMap<String, Double>();
			candidates = new HashMap<String, Individual>();
		}

		public LCAResolution(Resolution resolution) {
			this();
			if (resolution.candidates != null) {
				for (Individual candidate : resolution.candidates) {
					Double score = resolution.scores.get(resolution.candidates.indexOf(candidate));
					scores.put(candidate.getName(), score);
					candidates.put(candidate.getName(), candidate);
				}
			}
			resolved = resolution.resolved;
			omitted = resolution.omitted;
		}

		public void addCandidate(String candidateName, Double score) {
			scores.put(candidateName, score);
		}

		public void addCandidate(String candidateName, Double score, Individual candidate) {
			scores.put(candidateName, score);
			candidates.put(candidateName, candidate);
		}

		public boolean isCandidate(String candidateName) {
			return candidates.containsKey(candidateName);
		}

		public boolean hasUniqueCandidate() {
			return candidates.size() == 1;
		}

		public Set<String> getCandidates() {
			return candidates.keySet();
		}

		public String getUniqueCandidate() {
			return candidates.keySet().iterator().next();
		}

		public boolean isOmitted() {
			return omitted;
		}

		public boolean isResolved() {
			return resolved;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (String candidate : scores.keySet()) {
				sb.append(candidate).append("\t").append(scores.get(candidate)).append("\n");
			}
			return sb.toString();
		}
	}

	protected static final long serialVersionUID = 1L;
	protected Map<Integer, LCAResolution> resolutions = null;
	protected Map<Integer, LCAResolution> resolutionsWithUniqueCandidates = new HashMap<Integer, LCAResolution>();
	protected LearnerCentricAnalysis lca = null;

	public ResolutionResults() {
		resolutions = new HashMap<Integer, LCAResolution>();
	}

	public ResolutionResults(LearnerCentricAnalysis lca, AnalysisInContext analysis) {
		this();
		this.lca = lca;
		extractFromAnalysis(analysis);
	}

	public ResolutionResults(LearnerCentricAnalysis lca, Collection<AnalysisInContext> partialAnalyses) {
		this();
		this.lca = lca;
		for (AnalysisInContext analysis : partialAnalyses) {
			extractFromAnalysis(analysis);
		}
	}

	protected void extractFromAnalysis(AnalysisInContext analysis) {
		Map<Slot, Resolution> res = analysis.getResolutions();
		for (Slot slot : res.keySet()) {
			LCAResolution result = new LCAResolution(res.get(slot));
			resolutions.put(slot.getID(), result);
			if (result.hasUniqueCandidate()) {
				resolutionsWithUniqueCandidates.put(slot.getID(), result);
			}
		}
	}

	protected void addResult(Slot slot, String candidate, Double score) {
		if (!resolutions.containsKey(slot)) {
			resolutions.put(slot.getID(), new LCAResolution());
		}
		resolutions.get(slot.getID()).addCandidate(candidate, score);
	}

	public LCAResolution getResult(int slotID) {
		return resolutions.get(slotID);
	}

	public List<LCAResolution> getResolutionsWithUniqueCandidates() {
		return new ArrayList<LCAResolution>(resolutionsWithUniqueCandidates.values());
	}

	public List<String> getUniqueCandidates() {
		List<String> uniqueCandidates = new ArrayList<String>();
		for (LCAResolution res : resolutionsWithUniqueCandidates.values()) {
			uniqueCandidates.add(res.getUniqueCandidate());
		}
		return uniqueCandidates;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int slotID : resolutions.keySet()) {
			sb.append(lca.getSlot(slotID).toString()).append(" => \n");
			sb.append(resolutions.get(slotID).toString());
		}
		return sb.toString();
	}
}
