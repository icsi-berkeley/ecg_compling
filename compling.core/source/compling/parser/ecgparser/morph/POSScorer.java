package compling.parser.ecgparser.morph;

import java.util.Map;
import java.util.Set;

import compling.grammar.unificationgrammar.TypeSystemException;

public class POSScorer extends MAnalysisUnaryHashtableScorer<String> {
	public POSScorer(Map<String, Double> hashtable) {
		super(hashtable);
	}

	public double score(MAnalysis a) {
		Set<String> posTags = table.keySet();
		String posTag = null;
		try {
			for (String sup : a.getHeadCxn().getCxnTypeSystem().getAllSuperTypes(a.getHeadCxn().getName())) {
				if (posTags.contains(sup))
					posTag = sup;
			}
			if (posTag!=null)
				return score(posTag);
		} catch(TypeSystemException ex) {
			ex.printStackTrace();
			return Double.NEGATIVE_INFINITY;
		}
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double score(MAnalysis a, String inputWord) {
		// TODO Auto-generated method stub
		return 0;
	}
}
