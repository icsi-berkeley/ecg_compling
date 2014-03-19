package compling.parser.ecgparser.morph;

/**
 * Returns a score of 0.0 for every request.
 */
public class UniformScorer implements MAnalysisScorer {

	public UniformScorer() {
	}

	/**
	 * Given a morphological analysis and the word covered by that analysis, produce a score based on model parameters.
	 */
	public double score(MAnalysis a, String inputWord) {
		return 0.0;
	}

}