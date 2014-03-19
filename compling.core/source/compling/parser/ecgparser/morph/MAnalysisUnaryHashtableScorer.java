package compling.parser.ecgparser.morph;

import java.util.Map;

/**
 * Scores an MAnalysis by consulting a hashtable, presenting it with a key and retrieving a double (the score) The keys
 * must be acquired from the MAnalysis object
 * 
 * @author Nathan Schneider
 * 
 */
public abstract class MAnalysisUnaryHashtableScorer<K> extends MAnalysisHashtableScorer {
	public Map<K, Double> table;

	public MAnalysisUnaryHashtableScorer(Map<K, Double> hashtable) {
		super(hashtable);
		table = tbl;
	}

	public abstract double score(MAnalysis a);

	protected double score(K key) {
		return table.get(key);
	}
}
