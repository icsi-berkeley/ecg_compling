// =============================================================================
// File        : ContextFitter.java
// Author      : emok
// Change Log  : Created on Mar 18, 2008
//=============================================================================

package compling.learner.contextfitting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer.DSRole;
import compling.annotation.childes.ChildesLocalizer.Participant;
import compling.context.ContextModel;
import compling.context.MiniOntology;
import compling.grammar.unificationgrammar.FeatureStructureSet.Slot;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.grammar.unificationgrammar.TypeSystemNode;
import compling.grammar.unificationgrammar.UnificationGrammar.Role;
import compling.grammar.unificationgrammar.UnificationGrammar.SlotChain;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.AnalysisVerifier.Correctness;
import compling.learner.LearnerException;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.util.Pair;

//=============================================================================

public interface ContextFitter {

	public enum FittableType {
		DS(true),
		DISCOURSE_PARTTCIPANT(true),
		COMPLEX_PROCESS(false),
		STRUCTURED_SIMPLE_PROCESS(false),
		UNSTRUCTURED_SIMPLE_PROCESS(false),
		STRUCTURED_ELEMENT(true),
		UNSTRUCTURED_ELEMENT(true);

		boolean obeyRD;

		private FittableType(boolean obeyRD) {
			this.obeyRD = obeyRD;
		}
	}

	public static interface ContextFittingLocalizer {
		public String getParticipantLocalization(Participant participant);

		public String getDSRoleLocalization(DSRole role);
	}

	public static class ContextualFitComparator implements Comparator<ContextualFit> {
		public int compare(ContextualFit fit1, ContextualFit fit2) {
			double score1 = fit1.getScore();
			double score2 = fit2.getScore();
			return score1 == score2 ? 0 : score1 < score2 ? -1 : +1;
		}
	}

	public ContextualFit getContextualFit(LearnerCentricAnalysis lca);

	public List<String> getExpandedJointAttention();

	public List<String> getRdInvokedContextElements();

	public class ContextualFit {

		// FIXME: extra information needs to be added to the SemSpec when the best fit is found

		String id;
		Double fitScore = 0.0;
		LearnerCentricAnalysis currentLCA;
		boolean catastrophicallyImcompatible = false;
		TypeSystem<? extends TypeSystemNode> ontologyTypeSystem;
		boolean fitScoreUpToDate = true;

		Set<Integer> roots = new HashSet<Integer>();
		Map<Integer, String> individuals = new HashMap<Integer, String>();
		Map<Integer, Double> scores = new HashMap<Integer, Double>();
		Map<Pair<Integer, SlotChain>, String> additionalIndividuals = new HashMap<Pair<Integer, SlotChain>, String>();
		Map<Pair<Integer, SlotChain>, Double> additionalScores;

		Map<Integer, Correctness> verificationResults = new HashMap<Integer, Correctness>();

		static Logger logger = Logger.getLogger(ContextualFit.class.getName());

		public ContextualFit(LearnerCentricAnalysis lca, String id, TypeSystem<? extends TypeSystemNode> ontologyTypeSystem) {
			this.id = id;
			this.ontologyTypeSystem = ontologyTypeSystem;
			currentLCA = lca;
		}

		public void addCandidate(Integer slotID, String candidate, boolean isRoot) {
			if (isRoot) {
				roots.add(slotID);
			}
			individuals.put(slotID, candidate);

			if (candidate != null && !isCompatible(currentLCA.getTypeConstraint(slotID), candidate)) {
				setCatastrophicallyImcompatable();
			}
			fitScoreUpToDate = false;
		}

		// NOTE: the handling of additional slot relies on the assumption that in this implementation
		// of feature structure, roles that are unified by constraints in the schemas would be
		// present in the semspec.
		// (The assumption is not entirely true for roles in evoked schemas, but it'll do for now)

		public void addCandidate(Pair<Integer, SlotChain> additionalSlot, String candidate) {
			additionalIndividuals.put(additionalSlot, candidate);

			List<Role> chain = additionalSlot.getSecond().getChain();
			if (!isCompatible(chain.get(chain.size() - 1).getTypeConstraint(), candidate)) {
				setCatastrophicallyImcompatable();
			}
			fitScoreUpToDate = false;
		}

		protected boolean isCompatible(TypeConstraint slotTypeConstraint, String candidate) {
			if (slotTypeConstraint == null) {
				return true;
			}

			String candidateType = ontologyTypeSystem.getInternedString(ContextModel.getIndividualType(candidate));
			String slotType = ontologyTypeSystem.getInternedString(slotTypeConstraint.getType());
			if (candidateType == slotType) {
				return true;
			}
			try {
				boolean isSubtype = ontologyTypeSystem.subtype((candidateType),
						ontologyTypeSystem.getInternedString(slotType));
				if (isSubtype) {
					return true;
				}
				else {
					return false;
				}
			}
			catch (TypeSystemException tse) {
				// throw new LearnerException("bizarre error occurred while scoring a contextual fit", tse);
				// the slot must be a schema type that is only defined in the schema type system but not ontology type
				// system
				return false;
			}
		}

		public String getCandidate(Integer slotID) {
			return individuals.get(slotID);
		}

		public String getCandidate(Pair<Integer, SlotChain> additionalSlot) {
			return additionalIndividuals.get(additionalSlot);
		}

		public Set<String> getCandidates() {
			return new HashSet<String>(individuals.values());
		}

		public Double getScore() {
			if (scores.isEmpty()) {
				if (catastrophicallyImcompatible) {
					fitScore = Double.NEGATIVE_INFINITY;
				}
				else {
					fitScore = 0.0;
				}
			}
			else if (!fitScoreUpToDate) {
				tallyScore();
				fitScoreUpToDate = true;
			}
			return fitScore;
		}

		protected void tallyScore() {
			if (isCatastrophicallyImcompatible()) {
				fitScore = Double.NEGATIVE_INFINITY;
			}
			else {
				int numSlots = individuals.size();
				int numNullSlots = 0;
				double total = 0.0;

				for (Integer slotID : individuals.keySet()) {
					if (individuals.get(slotID) == null) {
						numNullSlots++;
					}
					else {
						total += scores.get(slotID);
					}
				}
				if (numSlots == 0 || numSlots == numNullSlots) {
					fitScore = 0.0;
				}
				else {
					double recall = total / numSlots;
					double precision = total / (numSlots - numNullSlots);
					fitScore = (2 * recall * precision) / (recall + precision);
				}
			}
		}

		public void assignScore(Integer slotID, Double score) {
			if (individuals.containsKey(slotID)) {
				scores.put(slotID, score);
			}
			else {
				throw new LearnerException("attempting to assign a contextual fit score to a non-existent slot");
			}
			fitScoreUpToDate = false;
		}

		public void assignScore(Pair<Integer, SlotChain> additionalSlot, Double score) {
			if (additionalIndividuals.containsKey(additionalSlot)) {
				additionalScores.put(additionalSlot, score);
			}
			else {
				throw new LearnerException("attempting to assign a contextual fit score to a non-existent slot");
			}
			fitScoreUpToDate = false;
		}

		public Correctness getVerificationResults(Integer slotID) {
			return verificationResults.get(slotID);
		}

		public void setVerificationResults(Integer slotID, Correctness correctness) {
			verificationResults.put(slotID, correctness);
		}

		public Set<Integer> getSlots() {
			return individuals.keySet();
		}

		public Set<Pair<Integer, SlotChain>> getAdditionalSlots() {
			return additionalIndividuals.keySet();
		}

		public Set<Integer> getRoots() {
			return roots;
		}

		public Double getScore(Integer slotID) {
			return scores.get(slotID);
		}

		public boolean isCatastrophicallyImcompatible() {
			return catastrophicallyImcompatible;
		}

		public void setCatastrophicallyImcompatable() {
			catastrophicallyImcompatible = true;
			fitScoreUpToDate = false;
		}

		public boolean isFitted(Integer slotID) {
			return individuals.containsKey(slotID);
		}

		public boolean isFitted(Pair<Integer, SlotChain> additionalSlot) {
			return additionalIndividuals.containsKey(additionalSlot);
		}

		public void copySlotFrom(ContextualFit that, Integer slotID) {
			individuals.put(slotID, that.individuals.get(slotID));
			scores.put(slotID, that.scores.get(slotID));
		}

		public void incorporate(ContextualFit that) {
			if (that != null) {
				if (!that.individuals.containsValue(null)) {
					individuals.putAll(that.individuals);
					roots.addAll(that.roots);
				}
				else {
					for (Integer slotID : that.individuals.keySet()) {
						if (that.individuals.get(slotID) != null || !individuals.containsKey(slotID)) {
							individuals.put(slotID, that.individuals.get(slotID));
							if (that.roots.contains(slotID)) {
								roots.add(slotID);
							}
						}
					}
				}
				catastrophicallyImcompatible |= that.catastrophicallyImcompatible;
				logger.finest("adding to " + id + " ...");
				logger.finest(that.toString());
			}
			else {
				logger.finest("nothing incorporated to " + id + " given a null ContextualFit");
			}
			fitScoreUpToDate = false;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (getScore() != null) {
				sb.append("Total score = ").append(getScore()).append("\n");
			}
			if (!individuals.isEmpty()) {
				for (Integer slotID : individuals.keySet()) {
					Slot slot = currentLCA.getSlot(slotID);
					sb.append(slotID).append("\t");
					sb.append(slot.toString());
					sb.append("\t<==\t").append(individuals.get(slotID)).append("\n");
				}
				sb.deleteCharAt(sb.length() - 1).deleteCharAt(sb.length() - 1);
			}
			return sb.toString();
		}
	}
}