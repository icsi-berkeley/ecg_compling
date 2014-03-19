package compling.util.probability;

import java.util.List;

/**
 * This interface is intended to describe Probability Distributions.
 * 
 * If you want to query for a marginal, don't pass any evidence in the evidence list
 * 
 * Note that this class only is defined for a single query variable and not for joint marginal/conditional
 * probabilities.
 * 
 * i.e. P(v|evidence) is ok, but P(v_1, v_2, ..., v_i | evidence) is not ok
 */
public interface ProbabilityDistribution<VARTYPE, EVIDENCETYPE> {

	public double getConditionalProbability(VARTYPE var, List<EVIDENCETYPE> evidence);

	public double getConditionalLogProbability(VARTYPE var, List<EVIDENCETYPE> evidence);

}
