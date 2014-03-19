// =============================================================================
// File        : GrammarCost.java
// Author      : emok
// Change Log  : Created on Apr 18, 2008
//=============================================================================

package compling.learner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import compling.annotation.childes.ChildesLocalizer;
import compling.grammar.ecg.Grammar;
import compling.grammar.ecg.Grammar.Construction;
import compling.grammar.unificationgrammar.TypeSystem;
import compling.grammar.unificationgrammar.TypeSystemException;
import compling.gui.AnalyzerPrefs;
import compling.gui.AnalyzerPrefs.AP;
import compling.parser.ecgparser.Analysis;
import compling.parser.ecgparser.ECGAnalyzer;
import compling.parser.ecgparser.NoECGAnalysisFoundException;
import compling.util.PriorityQueue;
import compling.utterance.Utterance;
import compling.utterance.Word;

//=============================================================================

public class MDLCost {

	final static int MEANLENGTH = 3;
	final static int MEANLENGTH_FACTORIAL = 6;

	private LearnerGrammar learnerGrammar = null;
	private double descriptionLength = 0.0;
	private boolean isUpToDate = false;

	final static double ALPHA = 0.95;

	static ECGAnalyzer analyzer = null;
	private static Logger logger = Logger.getLogger(MDLCost.class.getName());

	public MDLCost(LearnerGrammar grammar) {
		learnerGrammar = grammar;

		if (analyzer != null) {
			StringBuffer grammarParamsCxn = grammar.getConstructionalSubtypeTable().outputConstituentExpansionCountTable();
			StringBuffer grammarParamsSem = grammar.getSemanticSubtypeTable().outputSemanticFillerCostTable();
			analyzer.loadNewGrammar(grammar.getGrammar(), grammarParamsCxn, grammarParamsSem);
		}
		isUpToDate = false;
	}

	private void instantiateAnalyzer(LearnerGrammar grammar) throws IOException {
		AnalyzerPrefs prefs = (AnalyzerPrefs) grammar.getGrammar().getPrefs().clone();
		prefs.setSetting(AP.ANALYZE_IN_CONTEXT, "false");
		analyzer = new ECGAnalyzer(grammar.getGrammar(), prefs);
	}

	public void noLongerUpToDate() {
		isUpToDate = false;
	}

	public double getDescriptionLength() throws IOException {
		if (!isUpToDate) {
			descriptionLength = ALPHA * dataEncodingLength() + (1 - ALPHA) * grammarEncodingLength();
			isUpToDate = true;
		}
		return descriptionLength;
	}

	// description length of data
	// a cache of a few sentences to analyze?
	private double dataEncodingLength() throws IOException {

		if (learnerGrammar.getCacheUtterances().isEmpty())
			return 0.0;

		double dataLength = 0.0;
		if (analyzer == null) {
			instantiateAnalyzer(learnerGrammar);
		}

		for (Utterance<Word, String> utterance : learnerGrammar.getCacheUtterances()) {
			try {
				PriorityQueue<List<Analysis>> a = analyzer.getBestPartialParses(utterance);
				double cost = -a.getPriority();
				dataLength += cost;
				logger.finer("utterance cost = " + cost);
			}
			catch (NoECGAnalysisFoundException neafe) {
				logger.warning("when calculating desciption length, EXCEPTIONALLY BAD ANALYSIS encountered: "
						+ utterance.toString());
			}
		}
		return dataLength;
	}

	// description length of grammar

	private double grammarEncodingLength() {

		Grammar grammar = learnerGrammar.getGrammar();

		TypeSystem<Construction> cxnTS = grammar.getCxnTypeSystem();
		String wordType = cxnTS.getInternedString(ChildesLocalizer.WORD);
		String intjType = cxnTS.getInternedString(ChildesLocalizer.INTERJECTION);

		List<Construction> lexicalCxns = new ArrayList<Construction>();
		List<Construction> abstractCxns = new ArrayList<Construction>();
		List<Construction> phrasalCxns = new ArrayList<Construction>();

		for (Construction c : grammar.getAllConstructions()) {
			try {
				if (// grammar.isLexicalConstruction(c) ||
				(c.isConcrete() && (cxnTS.subtype(c.getName(), wordType) || cxnTS.subtype(c.getName(), intjType)))
						|| (!c.isConcrete() && c.getName().contains(ChildesLocalizer.VARIANT_SUFFIX))) {
					// lexical (morpheme, word, pronounciational variants)
					lexicalCxns.add(c);
				}
				else if (!c.isConcrete()) {
					// abstract constructions
					abstractCxns.add(c);
				}
				else if (!learnerGrammar.getGeneralizationHistory().hasBeenGeneralized(c.getName())) {
					// other phrasal constructions
					phrasalCxns.add(c);
				}
			}
			catch (TypeSystemException tse) {
				logger.warning(tse.getLocalizedMessage());
			}
		}

		double lexicalLength = lexicalCxns.size() * Math.log(lexicalCxns.size()); // include this or no? esp. for
																											// incremental lexical addition case
		double abstractLength = abstractCxns.size() * Math.log(abstractCxns.size());
		double phrasalLength = phrasalCxns.size() * Math.log(phrasalCxns.size());
		for (Construction c : phrasalCxns) {
			phrasalLength += descriptionLength(c);
		}

		logger.finer("grammar cost = " + lexicalLength + " " + abstractLength + " " + phrasalLength);
		return lexicalLength + abstractLength + phrasalLength;
	}

	private double descriptionLength(Construction phrasalCxn) {
		int u = phrasalCxn.getConstructionalBlock().getElements().size();
		double p = Math.exp(-u) * Math.pow(u, MEANLENGTH) / MEANLENGTH_FACTORIAL;
		return -Math.log(p);
	}

}
