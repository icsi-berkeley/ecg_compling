package compling.parser.ecgparser.morph;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import compling.grammar.unificationgrammar.TypeSystemException;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser;
import compling.util.Counter;
import compling.util.Pair;
import compling.util.fileutil.TextFileLineIterator;

public class ParamFileWordGivenPOS implements MAnalysisScorer {
	Map<String, Counter<String>> table = new IdentityHashMap<String, Counter<String>>(); // POS_cxn_type_name =>
																														// dist_over_words

	public ParamFileWordGivenPOS(MGrammarWrapper grammar, String paramFilePath) throws IOException {
		this(grammar, new TextFileLineIterator(paramFilePath));
	}

	public ParamFileWordGivenPOS(MGrammarWrapper grammar, TextFileLineIterator lineIterator) {
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			if (line == "") {
				continue;
			}
			ParamLineParser.ParamContainer pc = ParamLineParser.parseLine(line);
			Counter<String> counter = new Counter<String>();
			String key = grammar.getConstruction(pc.structureName).getName();
			if (key == null) {
				// throw new ParserException("Unknown cxntype: "+key.getName()+ " in params file");
			}
			else {
				table.put(key, counter);
				for (Pair<String, Double> pair : pc.params) {
					String word = pair.getFirst();
					if (word == null) {
						// throw new ParserException("Unknown cxn: "+pair.getFirst()+ " in params file");
					}
					else {
						counter.setCount(word, Math.log(pair.getSecond()));
					}
				}
			}
		}
	}

	public double getScore(String posCxn, String word) {
		if (table.get(posCxn) != null && table.get(posCxn).containsKey(word)) {
			return table.get(posCxn).getCount(word);
		}
		else {
			return Double.NEGATIVE_INFINITY;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(
				"ParamFile Word Given POS\n----------------------------------------------------\n");
		for (String cxn : table.keySet()) {
			sb.append(cxn).append(": ");
			for (String word : table.get(cxn).keySet()) {
				sb.append(word).append("[").append(table.get(cxn).getCount(word)).append("]  ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Given a morphological analysis and the word covered by that analysis, produce a score based on model parameters.
	 */
	public double score(MAnalysis a, String inputWord) {
		Set<String> posTags = table.keySet();
		String posTag = null;
		try {
			for (String sup : a.getHeadCxn().getCxnTypeSystem().getAllSuperTypes(a.getHeadCxn().getName())) {
				for (String t : posTags) {
					if (t.equals(sup)) {
						posTag = t;
						break;
					}
				}
			}
			if (posTag != null)
				return getScore(posTag, inputWord);
		}
		catch (TypeSystemException ex) {
			ex.printStackTrace();
			return Double.NEGATIVE_INFINITY;
		}
		return Double.NEGATIVE_INFINITY;
	}

}