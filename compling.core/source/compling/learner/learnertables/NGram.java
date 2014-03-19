// =============================================================================
// File        : NGram.java
// Author      : emok
// Change Log  : Created on Apr 24, 2008
//=============================================================================

package compling.learner.learnertables;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.UnificationGrammar.TypeConstraint;
import compling.learner.featurestructure.LearnerCentricAnalysis;
import compling.parser.ecgparser.CxnalSpan;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser;
import compling.parser.ecgparser.LeftCornerParserTablesCxn.ParamLineParser.ParamContainer;
import compling.util.Counter;
import compling.util.LookupTable;
import compling.util.MapSet;
import compling.util.Pair;
import compling.util.fileutil.TextFileLineIterator;

//=============================================================================

public class NGram {

	private static final String UNIGRAM = "//Unigram";
	private static final String BIGRAM = "//Bigram";
	private static final String LEXICALIZED_UNIGRAM = "//LexicalizedUnigram";
	private static final String LEXICALIZED_BIGRAM = "//LexicalizedBigram";

	private static final String START_SYMBOL = "STARTSYMBOL";
	private static final String END_SYMBOL = "ENDSYMBOL";
	private static final TypeConstraint START_TYPE = new TypeConstraint(START_SYMBOL, null);
	private static final TypeConstraint END_TYPE = new TypeConstraint(END_SYMBOL, null);

	private Grammar currentGrammar;
	private TypeSystem<Construction> cxnTypeSystem = null;

	private Counter<TypeConstraint> frequencies = new Counter<TypeConstraint>();
	private LookupTable<TypeConstraint, TypeConstraint> bigram = new LookupTable<TypeConstraint, TypeConstraint>();

	private Counter<String> lexicalizedFrequencies = new Counter<String>();
	private LookupTable<String, String> lexicalizedBigram = new LookupTable<String, String>();

	public NGram(Grammar grammar) {
		currentGrammar = grammar;
		cxnTypeSystem = currentGrammar.getCxnTypeSystem();
	}

	public NGram(Grammar grammar, NGram oldNGram) {
		this(grammar);

		for (TypeConstraint oldTC : oldNGram.frequencies.keySet()) {
			if (oldTC != null) {// FIXME: don't knwo why there are nulls in there
				TypeConstraint newTC = getCurrentTypeConstraint(oldTC.getType());
				if (newTC != null) {
					frequencies.setCount(newTC, oldNGram.frequencies.getCount(oldTC));
				}
			}
		}

		for (TypeConstraint oldTC1 : oldNGram.bigram.keySet()) {
			if (oldTC1 != null) {// FIXME: don't knwo why there are nulls in there
				TypeConstraint newTC1 = getCurrentTypeConstraint(oldTC1.getType());
				if (newTC1 != null) {
					for (TypeConstraint oldTC2 : oldNGram.bigram.get(oldTC1).keySet()) {
						if (oldTC2 != null) {// FIXME: don't knwo why there are nulls in there
							TypeConstraint newTC2 = getCurrentTypeConstraint(oldTC2.getType());
							if (newTC2 != null) {
								bigram.put(newTC1, newTC2, oldNGram.bigram.get(oldTC1, oldTC2));
							}
						}
					}
				}
			}
		}

		for (String word : oldNGram.lexicalizedFrequencies.keySet()) {
			lexicalizedFrequencies.incrementCount(word, oldNGram.lexicalizedFrequencies.getCount(word));
		}
		lexicalizedBigram.putAll(oldNGram.lexicalizedBigram);
	}

	public NGram(Grammar grammar, TextFileLineIterator nGramTableIterator) {
		this(grammar);
		int mode = 0;

		while (nGramTableIterator.hasNext()) {
			String line = nGramTableIterator.next();
			if (line == "") {
				continue;
			}

			if (line.startsWith(UNIGRAM)) {
				mode = 0;
			}
			else if (line.startsWith(BIGRAM)) {
				mode = 1;
			}
			else if (line.startsWith(LEXICALIZED_UNIGRAM)) {
				mode = 2;
			}
			else if (line.startsWith(LEXICALIZED_BIGRAM)) {
				mode = 3;
			}
			else {
				if (mode == 0) {
					StringTokenizer st = new StringTokenizer(line);
					String name = st.nextToken();
					TypeConstraint constructionType = getCurrentTypeConstraint(name);
					if (constructionType != null) {
						String freq = st.nextToken();
						frequencies.incrementCount(constructionType, Double.valueOf(freq));
					}
				}
				else if (mode == 1) {
					ParamContainer pc = ParamLineParser.parseLine(line);
					TypeConstraint constructionType = getCurrentTypeConstraint(pc.structureName);
					if (constructionType != null) {
						for (Pair<String, Double> pair : pc.params) {
							TypeConstraint nextType = getCurrentTypeConstraint(pair.getFirst());
							if (nextType != null) {
								bigram.incrementCount(constructionType, nextType, pair.getSecond().intValue());
							}
						}
					}
				}
				else if (mode == 2) {
					StringTokenizer st = new StringTokenizer(line);
					String name = st.nextToken();
					String freq = st.nextToken();
					lexicalizedFrequencies.incrementCount(name, Double.valueOf(freq));
				}
				else if (mode == 3) {
					ParamContainer pc = ParamLineParser.parseLine(line);
					for (Pair<String, Double> pair : pc.params) {
						lexicalizedBigram.incrementCount(pc.structureName, pair.getFirst(), pair.getSecond().intValue());
					}
				}
			}
		}
	}

	public void clear() {
		frequencies = new Counter<TypeConstraint>();
		bigram.clear();
		lexicalizedFrequencies = new Counter<String>();
		lexicalizedBigram.clear();
	}

	public void updateTable(LearnerCentricAnalysis lca) {

		List<String> utterance = lca.getUtteranceAnalyzed().getText();
		if (utterance.size() > 0) {

			MapSet<Integer, CxnalSpan> indexedSpans = new MapSet<Integer, CxnalSpan>();
			for (CxnalSpan span : lca.getCxnalSpans().values()) {
				if (!span.gappedOut() && !span.omitted()) {
					indexedSpans.put(span.getLeft(), span);
				}
			}

			// update frequencies and constructional bigram
			// the constructional bigram is not a true probability distribution because the events should sum up to more
			// than 1
			for (CxnalSpan span : indexedSpans.allValues()) {
				TypeConstraint lastSpan = getCurrentTypeConstraint(span.getType().getName());
				frequencies.incrementCount(lastSpan, 1);
				if (indexedSpans.containsKey(span.getRight())) {
					for (CxnalSpan next : indexedSpans.get(span.getRight())) {
						TypeConstraint nextSpan = getCurrentTypeConstraint(next.getType().getName());
						bigram.incrementCount(lastSpan, nextSpan, 1);
					}
				}
				if (span.getLeft() == 0) {
					bigram.incrementCount(START_TYPE, lastSpan, 1);
				}
				if (span.getRight() == utterance.size()) {
					bigram.incrementCount(lastSpan, END_TYPE, 1);
				}
			}

			// update lexicalized bigram
			Iterator<String> i = utterance.listIterator();
			String lastWord = i.next();
			lexicalizedFrequencies.incrementCount(lastWord, 1);
			lexicalizedBigram.incrementCount(START_SYMBOL, lastWord, 1);
			while (i.hasNext()) {
				String nextWord = i.next();
				lexicalizedFrequencies.incrementCount(nextWord, 1);
				lexicalizedBigram.incrementCount(lastWord, nextWord, 1);
				lastWord = nextWord;
			}
			lexicalizedBigram.incrementCount(lastWord, END_SYMBOL, 1);
		}
	}

	private TypeConstraint getCurrentTypeConstraint(String cxnName) {
		TypeConstraint type = cxnTypeSystem.getCanonicalTypeConstraint(cxnName);
		if (type == null && cxnName.equals(START_SYMBOL)) {
			type = START_TYPE;
		}
		else if (type == null && cxnName.equals(END_SYMBOL)) {
			type = END_TYPE;
		}
		return type;
	}

	public int getUnigram(String typeName) {
		return ((Double) frequencies.getCount(getCurrentTypeConstraint(typeName))).intValue();
	}

	public int getBigram(String n0, String n1) {
		return bigram.getCount(getCurrentTypeConstraint(n0), getCurrentTypeConstraint(n1));
	}

	public int getLexicalizedUnigram(String n) {
		return ((Double) lexicalizedFrequencies.getCount(n)).intValue();
	}

	public int getLexicalizedBigram(String n0, String n1) {
		return lexicalizedBigram.getCount(n0, n1);
	}

	public Counter<TypeConstraint> getFrequencies() {
		return frequencies;
	}

	public LookupTable<TypeConstraint, TypeConstraint> getBigram() {
		return bigram;
	}

	public void outputNGram(File output) throws IOException {
		PrintStream ps = new PrintStream(output);
		ps.println("\n" + UNIGRAM);
		ps.println(outputUnigram());
		ps.println("\n" + BIGRAM);
		ps.println(outputBigram());
		ps.println("\n" + LEXICALIZED_UNIGRAM);
		ps.println(outputLexicalizedUnigram());
		ps.println("\n" + LEXICALIZED_BIGRAM);
		ps.println(outputLexicalizedBigram());
		ps.close();
	}

	public StringBuffer outputUnigram() {
		StringBuffer sb = new StringBuffer();
		List<TypeConstraint> keys = new ArrayList<TypeConstraint>(frequencies.keySet());
		Collections.sort(keys, new TypeConstraintComparator());
		for (TypeConstraint tc : keys) {
			sb.append(tc.getType()).append("\t").append(frequencies.getCount(tc)).append("\n");
		}
		return sb;
	}

	public StringBuffer outputBigram() {
		StringBuffer sb = new StringBuffer();
		List<TypeConstraint> keys = new ArrayList<TypeConstraint>(bigram.keySet());
		Collections.sort(keys, new TypeConstraintComparator());
		for (TypeConstraint tc : keys) {
			sb.append(tc.getType()).append("\t");
			for (TypeConstraint tc2 : bigram.get(tc).keySet()) {
				sb.append(tc2.getType()).append(":").append(bigram.getCount(tc, tc2)).append("\t");;
			}
			sb.append("\n");
		}
		return sb;
	}

	public StringBuffer outputLexicalizedUnigram() {
		StringBuffer sb = new StringBuffer();
		List<String> keys = new ArrayList<String>(lexicalizedFrequencies.keySet());
		Collections.sort(keys);
		for (String t : keys) {
			sb.append(t).append("\t").append(lexicalizedFrequencies.getCount(t)).append("\n");
		}
		return sb;
	}

	public StringBuffer outputLexicalizedBigram() {
		StringBuffer sb = new StringBuffer();
		List<String> keys = new ArrayList<String>(lexicalizedBigram.keySet());
		Collections.sort(keys);
		for (String t : keys) {
			sb.append(t).append("\t");
			for (String t2 : lexicalizedBigram.get(t).keySet()) {
				sb.append(t2).append(":").append(lexicalizedBigram.getCount(t, t2)).append("\t");;
			}
			sb.append("\n");
		}
		return sb;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n UNIGRAM: \n").append(outputUnigram());
		sb.append("\n BIGRAM: \n").append(outputBigram());
		sb.append("\n LEXICALIZED_UNIGRAM: \n").append(outputLexicalizedUnigram());
		sb.append("\n LEXICALIZED_BIGRAM: \n").append(outputLexicalizedBigram());
		return sb.toString();
	}

	public static class TypeConstraintComparator implements Comparator<TypeConstraint> {
		public int compare(TypeConstraint t1, TypeConstraint t2) {
			return t1.getType().compareTo(t2.getType());
		}
	}
}
