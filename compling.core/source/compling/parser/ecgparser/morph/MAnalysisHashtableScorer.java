package compling.parser.ecgparser.morph;

import java.util.Map;

public abstract class MAnalysisHashtableScorer implements MAnalysisScorer {

	Map tbl;

	public MAnalysisHashtableScorer(Map hashtable) {
		tbl = hashtable;
	}

	public abstract double score(MAnalysis a);

}
