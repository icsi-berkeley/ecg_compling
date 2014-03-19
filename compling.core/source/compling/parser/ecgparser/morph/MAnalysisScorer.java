package compling.parser.ecgparser.morph;

public interface MAnalysisScorer {
	/**
	 * Given a morphological analysis and the word covered by that analysis, produce a score based on model parameters.
	 */
	double score(MAnalysis a, String inputWord);
}
