/**
 *
 */
package compling.gui.grammargui.model;

import java.io.IOException;
import java.util.List;

import compling.grammar.ecg.Grammar;
import compling.parser.ecgparser.ECGAnalyzer;

/**
 * A PrefsManager represents the data shown by GrammarBrowser objects
 * 
 * @author lucag
 */
public class GrammarBrowserModel {

	private Grammar grammar;
	private List<String> sentences = null;
	private ECGAnalyzer analyzer = null;

	public GrammarBrowserModel(Grammar grammar) {
		this.grammar = grammar;
//		browser = browser;
	}

	/**
	 * @return the grammar
	 */
	public Grammar getGrammar() {
		return grammar;
	}

	/**
	 * @param grammar
	 *           - the grammar to set
	 */
	public void setGrammar(Grammar grammar) {
		this.grammar = grammar;
		this.analyzer = null;

//		notifyObservers();
	}

	/**
	 * @return the sentenceText
	 */
	public List<String> getSentences() {
		return sentences;
	}

	/**
	 * @param sentences
	 *           the sentence text to set
	 */
	public void setSentences(List<String> sentences) {
		this.sentences = sentences;
//		notifyObservers();
	}

	/**
	 * @return the analyzer
	 * @throws IOException
	 */
	public ECGAnalyzer getAnalyzer() throws IOException {
		if (analyzer == null)
			analyzer = new ECGAnalyzer(grammar);

		return analyzer;
	}

}
