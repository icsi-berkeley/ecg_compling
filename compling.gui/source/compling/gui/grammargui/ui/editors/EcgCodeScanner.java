package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import compling.gui.grammargui.model.ColorProvider;

public class EcgCodeScanner extends RuleBasedScanner {

	private static EcgCodeScanner instance;

	/**
	 * Returns the singleton Java code scanner.
	 * 
	 * @return the singleton Java code scanner
	 */
	public static RuleBasedScanner instance() {
		if (instance == null)
			instance = new EcgCodeScanner(ColorProvider.instance());
		return instance;
	}

	private static final String[] keywords = { "constructional", "construction", "optional", "extraposed",
			"constraints", "subcase", "schema", "form", "meaning", "feature", "semantic", "constituents", "before",
			"ignore", "meets", "evokes", "as", "of", "abstract", "general", "roles", "setcurrentinterval", "defs:",
			"type", "inst", "insts:", "rel", "rem", "fun", "ind", "fil", "eq", "transient", "persistent", "nonblocking",
			"map", "situation", "sub", };

	private static final String[] operators = { "<--", "<->", "<-->", "[", "]", ".", ",", ":", "@", };

	public EcgCodeScanner(ColorProvider provider) {
		IToken keyword = new Token(new TextAttribute(provider.getColor(ColorProvider.KEYWORD), null, SWT.BOLD));
		IToken operator = new Token(new TextAttribute(provider.getColor(ColorProvider.OPERATOR)));
		IToken string = new Token(new TextAttribute(provider.getColor(ColorProvider.STRING)));
		IToken comment = new Token(new TextAttribute(provider.getColor(ColorProvider.COMMENT)));
		IToken other = new Token(new TextAttribute(provider.getColor(ColorProvider.DEFAULT)));

		List<IRule> rules = new ArrayList<IRule>();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", comment));

		// Add rule for strings
		rules.add(new SingleLineRule("\"", "\"", string, '\\'));

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new WhitespaceDetector()));

		// Add word rule for keywords, types, and constants.
		WordRule wordRule = new WordRule(new EcgWordDetector(), other, true);
		for (int i = 0; i < keywords.length; i++)
			wordRule.addWord(keywords[i], keyword);
		for (int i = 0; i < operators.length; i++)
			wordRule.addWord(operators[i], operator);

		rules.add(wordRule);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
