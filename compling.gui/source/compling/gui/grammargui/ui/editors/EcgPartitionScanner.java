package compling.gui.grammargui.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

public class EcgPartitionScanner extends RuleBasedPartitionScanner {

	public final static String ECG_PARTITIONING = "__ecg_partitioning";
	public final static String ECG_COMMENT = "__ecg_comment"; //$NON-NLS-1$
	public final static String ECG_MULTILINE_COMMENT = "__ecg_multiline_comment"; //$NON-NLS-1$
	public final static String[] ECG_PARTITION_TYPES = new String[] { ECG_COMMENT, ECG_MULTILINE_COMMENT };

	private static EcgPartitionScanner instance;

	/**
	 * Return a scanner for creating ECG partitions.
	 * 
	 * @return a scanner for creating ECG partitions
	 */
	public static EcgPartitionScanner instance() {
		if (instance == null)
			instance = new EcgPartitionScanner();
		return instance;
	}

	private EcgPartitionScanner() {
		super();
		IToken comment = new Token(ECG_MULTILINE_COMMENT);

		List<IRule> rules = new ArrayList<IRule>();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", Token.UNDEFINED));

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\'));
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\'));

		// Add rules for multi-line comments
		rules.add(new MultiLineRule("/*", "*/", comment, (char) 0, true));

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}

}
