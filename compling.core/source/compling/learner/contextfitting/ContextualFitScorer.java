// =============================================================================
//File        : ContextualFitScorer.java
//Author      : emok
//Change Log  : Created on Jun 21, 2007
//=============================================================================

package compling.learner.contextfitting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import compling.annotation.childes.ChildesLocalizer;
import compling.context.ContextModel;
import compling.context.ContextUtilities;
import compling.context.MiniOntologyQueryAPI.SimpleQuery;
import compling.learner.LearnerException;
import compling.learner.contextfitting.ContextFitter.ContextualFit;

//=============================================================================

public interface ContextualFitScorer {

	public ContextualFit score(ContextualFit fit, ContextualFit committedFit);

	abstract static public class ContextualFitScorerFactory {
		abstract public ContextualFitScorer makeScorer(ContextModel contextModel, ContextFitter fitter);
	}

	public static class RecencyScorerFactory extends ContextualFitScorerFactory {
		public ContextualFitScorer makeScorer(ContextModel contextModel, ContextFitter fitter) {
			return new RecencyScorer(contextModel, fitter.getExpandedJointAttention());
		}
	}

	public static class CoherenceScorerFactory extends ContextualFitScorerFactory {
		public CoherenceScorer makeScorer(ContextModel contextModel, ContextFitter fitter) {
			return new CoherenceScorer(fitter.getRdInvokedContextElements());
		}
	}

	public static class NestedScorerFactory extends ContextualFitScorerFactory {
		public NestedScorer makeScorer(ContextModel contextModel, ContextFitter fitter) {
			return new NestedScorer(contextModel, fitter.getExpandedJointAttention(), fitter.getRdInvokedContextElements());
		}
	}

	public static class RecencyScorer implements ContextualFitScorer {

		private ContextModel contextModel = null;
		private Collection<String> jointAttention = null;
		private static final double alpha = -10.0;

		public RecencyScorer(ContextModel contextModel, Collection<String> jointAttention) {
			this.contextModel = contextModel;
			this.jointAttention = jointAttention;
		}

		public ContextualFit score(ContextualFit fit, ContextualFit committedFit) {
			for (Integer slotID : fit.getSlots()) {
				String candidate = fit.getCandidate(slotID);
				int lag = recencyCheck(candidate);
				double oldScore = fit.getScore(slotID) == null ? 1.0 : fit.getScore(slotID);
				fit.assignScore(slotID, oldScore * Math.exp(lag / alpha));
			}
			return fit;
		}

		protected int recencyCheck(String candidate) {
			if (candidate == null) {
				return 0;
			}
			String name = ContextModel.getIndividualName(candidate);
			if (jointAttention.contains(candidate)) {
				return 0; // don't penalize for things that are accessible right in the transcript setting.
			}

			String currentInterval = contextModel.getMiniOntology().getCurrentIntervalName();
			Set<String> timestamps = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(
					ChildesLocalizer.TIMESTAMP, currentInterval, "?t"), false));
			verifyTimestamp(timestamps, currentInterval);
			int timestamp = Integer.valueOf(timestamps.iterator().next().replaceAll("\"", ""));

			Set<String> candidateTimestamps = ContextUtilities.collapseResults(contextModel.query(new SimpleQuery(
					ChildesLocalizer.TIMESTAMP, name, "?t"), true));
			verifyTimestamp(candidateTimestamps, candidate);
			int candidateTimestamp = Integer.valueOf(candidateTimestamps.iterator().next().replaceAll("\"", ""));

			int lag = timestamp - candidateTimestamp;
			return lag > 0 ? lag : 0;
		}

		protected void verifyTimestamp(Set<String> timestamps, String interval) {
			if (timestamps.size() > 1) {
				throw new LearnerException("More than one timestamp found for " + interval);
			}
			if (timestamps.isEmpty()) {
				throw new LearnerException("No timestamps found for " + interval);
			}
		}
	}

	public static class CoherenceScorer implements ContextualFitScorer {

		private static final double penalty = 0.1;
		Set<String> freepass = null;

		public CoherenceScorer(Collection<String> freepass) {
			this.freepass = new HashSet<String>(freepass);
		}

		public ContextualFit score(ContextualFit fit, ContextualFit committedFit) {
			Set<String> committedFitCandidates = committedFit == null ? new HashSet<String>() : committedFit
					.getCandidates();
			Set<String> fitCandidates = new HashSet<String>();

			for (Integer slotID : fit.getSlots()) {
				String candidate = fit.getCandidate(slotID);
				if (!freepass.contains(candidate) && !committedFitCandidates.contains(candidate)
						&& !fitCandidates.contains(candidate)) {
					double oldScore = fit.getScore(slotID) == null ? 1.0 : fit.getScore(slotID);
					fit.assignScore(slotID, oldScore - penalty);
				}
				else if (fit.getScore(slotID) == null) {
					fit.assignScore(slotID, 1.0);
				}
				fitCandidates.add(candidate);
			}
			return fit;
		}
	}

	public static class NestedScorer implements ContextualFitScorer {

		CoherenceScorer coherence;
		RecencyScorer recency;

		public NestedScorer(ContextModel contextModel, Collection<String> jointAttention, Collection<String> freepass) {
			coherence = new CoherenceScorer(freepass);
			recency = new RecencyScorer(contextModel, jointAttention);
		}

		public ContextualFit score(ContextualFit fit, ContextualFit committedFit) {
			return coherence.score(recency.score(fit, committedFit), committedFit);
		}

	}
}
